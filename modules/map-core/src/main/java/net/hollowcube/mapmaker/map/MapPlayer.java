package net.hollowcube.mapmaker.map;

import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.LongPriorityQueue;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.hollowcube.command.CommandManager;
import net.hollowcube.command.util.CommandHandlingPlayer;
import net.hollowcube.common.math.Quaternion;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.compat.moulberrytweaks.debugrender.DebugShape;
import net.hollowcube.compat.moulberrytweaks.packets.ClientboundDebugRenderAddPacket;
import net.hollowcube.compat.moulberrytweaks.packets.ClientboundDebugRenderRemovePacket;
import net.hollowcube.mapmaker.map.block.CollidableBlock;
import net.hollowcube.mapmaker.map.block.ghost.GhostBlockHolder;
import net.hollowcube.mapmaker.map.command.DebugRenderersCommand;
import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.hollowcube.mapmaker.map.event.entity.Map2PlayerEnterEntityEvent;
import net.hollowcube.mapmaker.map.event.entity.Map2PlayerExitEntityEvent;
import net.hollowcube.mapmaker.map.item.vanilla.FireworkRocketItem;
import net.hollowcube.mapmaker.map.util.PlayerVisibility;
import net.hollowcube.mapmaker.map.util.spatial.SpatialObject;
import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.collision.PhysicsResult;
import net.minestom.server.collision.PhysicsUtils;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.*;
import net.minestom.server.entity.metadata.LivingEntityMeta;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.ConnectionState;
import net.minestom.server.network.PlayerProvider;
import net.minestom.server.network.packet.client.common.ClientPongPacket;
import net.minestom.server.network.packet.server.SendablePacket;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.common.PingPacket;
import net.minestom.server.network.packet.server.configuration.FinishConfigurationPacket;
import net.minestom.server.network.packet.server.play.*;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import net.minestom.server.snapshot.PlayerSnapshot;
import net.minestom.server.snapshot.SnapshotUpdater;
import net.minestom.server.utils.chunk.ChunkCache;
import net.minestom.server.utils.validate.Check;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class MapPlayer extends CommandHandlingPlayer {

    private static final Logger log = LoggerFactory.getLogger(MapPlayer.class);

    static {
        MinecraftServer.getPacketListenerManager().setPlayListener(ClientPongPacket.class, (packet, player) -> {
            if (player instanceof MapPlayer mp) mp.lastReceivedPingId = packet.id();
        });
    }

    public static @NotNull PlayerProvider simpleMapPlayer(@NotNull CommandManager commandManager) {
        return (connection, profile) -> new MapPlayer(connection, profile) {
            @Override public @NotNull CommandManager getCommandManager() {
                return commandManager;
            }
        };
    }

    // This field solves a pretty gross ordering issue with teleports, should investigate a better solution
    // in the future. In a parkour map, you can teleport into an action trigger which has a teleport action.
    // In this case, the order of operations (for example, with ender pearls is the following):
    // - pearl lands
    // - player.teleport(pearl.getPosition())
    //   - player.setPositionInternal()
    //     - run teleport actions
    //     - player.teleport(action.getPosition()) -> TELEPORTS ON CLIENT
    //       - player.setPositionInternal()
    //       - player.synchronizePositionAfterTeleport()
    //   - player.synchronizePositionAfterTeleport() -> TELEPORTS ON CLIENT
    // The result is that we send the wrong order of teleports and the client ends up in the pearl position
    // _not_ the region position (!!!). To get around this, we disable setPositionInternal position refreshes
    // during a teleport sequence, and update them on `refreshPosition()` which occurs (among other places)
    // at the end of synchronizePositionAfterTeleport after the teleport has been sent.
    // This does solve the issue but its very gross :(
    private final AtomicInteger pendingTeleports = new AtomicInteger(0);

    private final IntList ownedEntities = new IntArrayList();

    private final Object2IntMap<String> cooldownGroups = new Object2IntArrayMap<>();

    private Function<Player, PlayerVisibility> visibilityFunc = null;
    // entity id -> visibility ordinal
    private final Int2IntMap visibilityByEntity = new Int2IntArrayMap();

    private int riptideTicks = 0;

    // Only present sometimes (eg during riptide)
    private PhysicsResult nextPhysicsResult = null;

    private boolean canSendPose = true;

    // Need to key it because block handlers can be singletons
    record CollisionKey(CollidableBlock block, BlockVec pos) {
    }

    private final Map<CollisionKey, CollidableBlock.Collision> blocksTouching = new HashMap<>();
    private final Set<SpatialObject> objectsTouching = new HashSet<>();

    public MapPlayer(@NotNull PlayerConnection playerConnection, @NotNull GameProfile gameProfile) {
        super(playerConnection, gameProfile);
    }

    @Override
    public void startConfigurationPhase() {
        super.startConfigurationPhase();

        try {
            var mh = MethodHandles.privateLookupIn(Player.class, MethodHandles.lookup())
                    .findGetter(Player.class, "chunkQueue", LongPriorityQueue.class);
            var chunkQUeue = (LongPriorityQueue) mh.invoke(this);
            chunkQUeue.clear();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private boolean clientInConfigPhase = false;

    @Override
    public void sendPacket(@NotNull SendablePacket packet) {
        switch (packet) {
            // In case it is sent from somewhere we dont expect
            case PingPacket(int id) -> lastPingId.set(id);

            // Don't send pose packets to self if we aren't supposed to
            case EntityMetaDataPacket(var entity, var entries) when entity == this.getEntityId()
                    && entries.containsKey(MetadataDef.POSE.index())
                    && !canSendPose -> {
                var newEntries = new HashMap<>(entries);
                newEntries.remove(MetadataDef.POSE.index());
                super.sendPacket(new EntityMetaDataPacket(entity, newEntries));
                return;
            }

            case StartConfigurationPacket _ -> {
                clientInConfigPhase = true;
            }
            case FinishConfigurationPacket _ -> {
                clientInConfigPhase = false;
            }
            case ServerPacket.Play _ when clientInConfigPhase && !(packet instanceof ServerPacket.Configuration) -> {
                // We know about meta data, just checking for others.
                if (!(packet instanceof EntityMetaDataPacket)) {
                    log.error("Sending play packet while client will be in config phase! Packet: {}", packet,
                            new RuntimeException("synthetic exception for stacktrace"));
                }
                return; // Don't send it, they would be kicked
            }

            default -> {  // Do nothing
            }
        }

        super.sendPacket(packet);
    }

    @Override
    public void updateNewViewer(@NotNull Player player) {
        FutureUtil.assertTickThreadWarn(acquirable());
        super.updateNewViewer(player);
        updateVisibility(player);
    }

    @Override
    public void updateOldViewer(@NotNull Player player) {
        FutureUtil.assertTickThreadWarn(acquirable());
        super.updateOldViewer(player);
        visibilityByEntity.remove(player.getEntityId());
    }

    @Override
    public void sendPacketToViewers(@NotNull SendablePacket packet) {
        if (packet instanceof EntityMetaDataPacket metaPacket && interceptMetadataPacket(metaPacket))
            return; // Packet was handled by intercept function

        super.sendPacketToViewers(packet);
    }

    @Override
    public void tick(long time) {
        super.tick(time);

        physicsTick();
        riptideTick();
    }

    @Override
    public void remove(boolean permanent) {
        super.remove(permanent);
        visibilityByEntity.clear();
    }

    // region EXT: Pose
    public void setCanSendPose(boolean canSend) {
        this.canSendPose = canSend;
    }

    public boolean canSendPose() {
        return this.canSendPose;
    }

    /// We override this to use our own canFitWithBoundingBox implementation which correctly handles per-player blocks.
    @Override
    public void updatePose() {
        EntityPose oldPose = getPose();
        EntityPose newPose;

        // Figure out their expected state
        var meta = getEntityMeta();
        if (meta.isFlyingWithElytra()) {
            newPose = EntityPose.FALL_FLYING;
        } else if (meta instanceof LivingEntityMeta livingMeta && livingMeta.getBedInWhichSleepingPosition() != null) {
            newPose = EntityPose.SLEEPING;
        } else if (meta.isSwimming()) {
            newPose = EntityPose.SWIMMING;
        } else if (meta instanceof LivingEntityMeta livingMeta && livingMeta.isInRiptideSpinAttack()) {
            newPose = EntityPose.SPIN_ATTACK;
        } else if (isSneaking() && !isFlying()) {
            newPose = EntityPose.SNEAKING;
        } else {
            newPose = EntityPose.STANDING;
        }

        // TODO: does not correctly handle swimming.

        // Try to put them in their expected state, or the closest if they don't fit.
        if (canFitWithBoundingBox(newPose)) {
            // Use expected state
        } else if (canFitWithBoundingBox(EntityPose.SNEAKING)) {
            newPose = EntityPose.SNEAKING;
        } else if (canFitWithBoundingBox(EntityPose.SWIMMING)) {
            newPose = EntityPose.SWIMMING;
        } else {
            // If they can't fit anywhere, just use standing
            newPose = EntityPose.STANDING;
        }

        if (newPose != oldPose) setPose(newPose);

        if (this.hasTag(DebugRenderersCommand.DEBUG_PLAYER_BOUNDING_BOX)) {
            var box = getBoundingBox();
            var center = this.getPosition().add(0, box.height() / 2.0, 0);
            var id = Key.key("player_bounding_box");

            try {
                this.sendPacket(new BundlePacket());
                new ClientboundDebugRenderRemovePacket(id).send(this);
                new ClientboundDebugRenderAddPacket(
                        id,
                        new DebugShape.Box(
                                center, new Vec(box.width(), box.height(), box.depth()), Quaternion.ZERO,
                                0, 0xFFFF0000, 5
                        ),
                        DebugShape.FLAG_WIREFRAME, 10
                ).send(this);
            } finally {
                this.sendPacket(new BundlePacket());
            }
        }
    }

    // endregion

    public @NotNull CompletableFuture<Void> teleport(
            @NotNull Pos position, @NotNull Vec velocity, long @Nullable [] chunks,
            @MagicConstant(flagsFromClass = RelativeFlags.class) int flags,
            boolean shouldConfirm) {
        // See note on pendingTeleports
        pendingTeleports.incrementAndGet();
        return super.teleport(position, velocity, chunks, flags, shouldConfirm)
                .thenRun(pendingTeleports::decrementAndGet)
                .exceptionally(ex -> {
                    pendingTeleports.decrementAndGet();
                    throw new RuntimeException("Failed to teleport player " + getUsername(), ex);
                });
    }

    //region EXT: Ping

    private final AtomicInteger lastPingId = new AtomicInteger(0);
    private int lastReceivedPingId = -1;

    public int lastPingId() {
        return lastPingId.get();
    }

    public int lastReceivedPingId() {
        return lastReceivedPingId;
    }

    public void ping() {
        sendPacket(new PingPacket(lastPingId.incrementAndGet()));
    }

    //endregion

    //region EXT: Owned Entities

    public void addOwnedEntity(@NotNull Entity entity) {
        FutureUtil.assertTickThread();
        Check.argCondition(entity.isAutoViewable() || entity.getViewers().size() != 1 || !entity.getViewers().contains(this),
                "Owned entity must not be auto viewable and must be viewing only this player");

        this.ownedEntities.add(entity.getEntityId());
    }

    public void removeOwnedEntities() {
        FutureUtil.assertTickThread();

        var instance = getInstance();
        if (instance == null) return;

        var iter = ownedEntities.intIterator();
        while (iter.hasNext()) {
            var entity = instance.getEntityById(iter.nextInt());
            if (entity != null) entity.remove();
            iter.remove();
        }
    }

    //endregion

    //region EXT: Cooldown Management

    public boolean tryUseItem(@NotNull ItemStack itemStack) {
        var useCooldown = itemStack.get(DataComponents.USE_COOLDOWN);
        if (useCooldown == null || useCooldown.cooldownGroup() == null)
            return true;

        int cooldownEnd = cooldownGroups.getInt(useCooldown.cooldownGroup());
        if (cooldownEnd > getAliveTicks()) return false; // Still in cooldown

        int cooldownTicks = (int) (useCooldown.seconds() * 20);
        cooldownGroups.put(useCooldown.cooldownGroup(), (int) (getAliveTicks() + cooldownTicks));
        sendPacket(new SetCooldownPacket(useCooldown.cooldownGroup(), cooldownTicks));
        return true;
    }

    //endregion

    //region EXT: Per player visibility

    public void setVisibilityFunc(@Nullable Function<Player, PlayerVisibility> func) {
        this.visibilityFunc = func;
    }

    public void updateVisibility() {
        if (visibilityFunc == null) return;
        getViewers().forEach(this::updateVisibility);
    }

    /// @return true if the packet was handled, false otherwise
    private boolean interceptMetadataPacket(@NotNull EntityMetaDataPacket packet) {
        // We need to intercept the metadata packet to ensure the invisible flag is set if we are supposed to be invisible.

        // If the update isnt changing the base entity bitflags we can just forward it.
        var flagsEntry = packet.entries().get(0);
        if (flagsEntry == null) return false;

        var newEntries = new HashMap<>(packet.entries());
        var bits = ((Metadata.Entry<Byte>) flagsEntry).value().byteValue();
        bits |= 0x20;
        newEntries.put(0, Metadata.Byte(bits));
        var invisPacket = new EntityMetaDataPacket(packet.entityId(), newEntries);

        // Forward the original or new packet depending if we are invisible to them
        for (var viewer : getViewers()) {
            boolean invisible = visibilityByEntity.get(viewer.getEntityId()) != PlayerVisibility.VISIBLE.ordinal();
            viewer.sendPacket(invisible ? invisPacket : packet);
        }
        return true;
    }

    private void updateVisibility(@NotNull Player other) {
        if (visibilityFunc == null) return;

        var old = PlayerVisibility.VALUES[visibilityByEntity.getOrDefault(other.getEntityId(), 0)];
        var current = visibilityFunc.apply(other);
        if (old == current) return; // Do nothing

        other.sendPacket(new BundlePacket());
        try {
            if (old != PlayerVisibility.VISIBLE) {
                sendMetaInvisUpdate(other, false);
                if (old == PlayerVisibility.SPECTATOR)
                    sendGameModeUpdate(other, getGameMode());
            }

            if (current != PlayerVisibility.VISIBLE) {
                sendMetaInvisUpdate(other, true);
                if (current == PlayerVisibility.SPECTATOR)
                    sendGameModeUpdate(other, GameMode.SPECTATOR);
            }
        } finally {
            other.sendPacket(new BundlePacket());
        }

        visibilityByEntity.put(other.getEntityId(), current.ordinal());
    }

    private void sendMetaInvisUpdate(@NotNull Player player, boolean invisible) {
        byte metaFlags = Objects.requireNonNullElseGet((Metadata.Entry<Byte>) EntityMetadataStealer.steal(this).getEntries().get(0),
                () -> Metadata.Byte((byte) 0)).value();
        if (invisible) metaFlags |= 0x20; // Ensure the invisible flag is set
        player.sendPacket(new EntityMetaDataPacket(getEntityId(), Map.of(
                MetadataDef.ENTITY_FLAGS.index(),
                Metadata.Byte(metaFlags)
        )));
    }

    private void sendGameModeUpdate(@NotNull Player player, @NotNull GameMode gameMode) {
        var infoEntry = new PlayerInfoUpdatePacket.Entry(
                getUuid(), getUsername(), List.of(), false, 0,
                gameMode, // This is the relevant one, we are only updating the gamemode
                null, null, 0, false
        );
        player.sendPacket(new PlayerInfoUpdatePacket(PlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE, infoEntry));
    }

    //endregion

    //region EXT: Physics

    private boolean needsPhysicsPrediction() {
        return riptideTicks > 0;
    }

    private void physicsTick() {
        if (needsPhysicsPrediction()) {
            var velocity = Vec.fromPoint(position.sub(previousPosition));

            final Block.Getter chunkCache = new ChunkCache(instance, currentChunk, Block.STONE);
            nextPhysicsResult = PhysicsUtils.simulateMovement(position, velocity, boundingBox,
                    instance.getWorldBorder(), chunkCache, getAerodynamics(), hasNoGravity(), hasPhysics,
                    onGround, false, getLastPhysicsResult());
        } else {
            nextPhysicsResult = null;
        }
    }

    private boolean canFitWithBoundingBox(@NotNull EntityPose pose) {
        var instance = getInstance();
        if (instance == null) return true; // Sanity check not in an instance

        BoundingBox bb = this.getBoundingBox(pose);
        if (bb == null) return false;

        var position = getPosition();
        var iter = bb.getBlocks(getPosition());
        var blocks = Objects.requireNonNullElse(GhostBlockHolder.forPlayerOptional(this), instance);

        while (iter.hasNext()) {
            var posMut = iter.next();
            var pos = new Vec(posMut.x(), posMut.y(), posMut.z());
            var block = blocks.getBlock(pos, Block.Getter.Condition.TYPE);

            // For now just ignore scaffolding. It seems to have a dynamic bounding box, or is just parsed
            // incorrectly in MinestomDataGenerator.
            if (block.id() == Block.SCAFFOLDING.id()) continue;

            var collisionShape = block.registry().collisionShape();
            var hit = collisionShape != null && collisionShape.intersectBox(
                    position.sub(pos.blockX(), pos.blockY(), pos.blockZ()), bb);
            if (hit) return false;
        }

        return true;
    }

    private boolean isHittingNearbyEntity() {
        var instance = getInstance();
        if (instance == null) return false; // Sanity

        var position = getPosition();
        for (var entity : getInstance().getNearbyEntities(position, 2.0)) {
            if (!entity.isViewer(this)) continue;

            if (intersectEntity(position, entity))
                return true;
        }

        return false;
    }

    private @NotNull PhysicsResult getLastPhysicsResult() {
        try {
            class Holder {
                static MethodHandle getter = null;
            }
            if (Holder.getter == null) {
                Holder.getter = MethodHandles.privateLookupIn(Entity.class, MethodHandles.lookup())
                        .findGetter(Entity.class, "previousPhysicsResult", PhysicsResult.class);
            }

            return (PhysicsResult) Holder.getter.invokeExact((Entity) this);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            //noinspection DataFlowIssue
            return null; // Never hit
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    //endregion

    //region EXT: Trident Riptide

    public void beginRiptideAttack(int durationTicks) {
        this.riptideTicks = durationTicks;
    }

    public void cancelRiptideAttack() {
        if (this.riptideTicks <= 0) return;

        this.riptideTicks = 0;
        getPlayerMeta().setInRiptideSpinAttack(false);
    }

    private void riptideTick() {
        if (riptideTicks <= 0) return;

        riptideTicks--;

        // Stop if we hit a wall
        if (nextPhysicsResult.collisionX() || nextPhysicsResult.collisionZ())
            riptideTicks = 0;
        // Stop if we hit a player
        if (isHittingNearbyEntity()) riptideTicks = 0;

        if (riptideTicks <= 0) {
            getPlayerMeta().setInRiptideSpinAttack(false);
        }
    }

    //endregion

    //region EXT: Elytra Flight

    @Override
    public void setFlyingWithElytra(boolean isFlying) {
        super.setFlyingWithElytra(isFlying);
        if (!isFlying) FireworkRocketItem.removeRocket(this);
    }

    //endregion

    //region EXT: Collision Tree Handling

    public void resetTouchingState() {
        this.blocksTouching.clear();
        this.objectsTouching.clear();
    }

    public void updateTouchingState(MapWorld world, boolean callEvents) {
        updateTouchingBlocks(world, callEvents);
        updateTouchingMarkerEntities(world, callEvents);
    }

    @Override
    protected void setPositionInternal(@NotNull Pos newPosition, float headRotation) {
        super.setPositionInternal(newPosition, headRotation);

        // See note on pendingTeleports
        if (pendingTeleports.get() <= 0) {
            var world = MapWorld.forPlayer(this);
            if (world != null) updateTouchingState(world, true);
        }
    }

    @Override
    protected void synchronizePosition() {
        super.synchronizePosition();

        // See note on pendingTeleports
        if (pendingTeleports.get() > 0) {
            var world = MapWorld.forPlayer(this);
            if (world != null) updateTouchingState(world, true);
        }
    }

    private void updateTouchingBlocks(MapWorld world, boolean callEvents) {
        var instance = getInstance();
        if (instance == null || !isActive()) return; // Sanity check not in an instance

        final BoundingBox bb = this.getBoundingBox();
        if (bb == null) return;

        var newBlocks = new HashMap<CollisionKey, CollidableBlock.Collision>();

        var position = getPosition();
        var iter = bb.getBlocks(getPosition());
        while (iter.hasNext()) {
            var posMut = iter.next();
            var pos = new BlockVec(posMut.x(), posMut.y(), posMut.z());
            var chunk = instance.getChunkAt(pos);
            if (chunk == null || !chunk.isLoaded()) continue;
            var block = chunk.getBlock(pos, Block.Getter.Condition.CACHED);
            var handler = OpUtils.map(block, Block::handler);
            if (!(handler instanceof CollidableBlock collidableBlock))
                continue;

            var hit = bb.intersectBox(position.sub(pos.blockX() + 0.5, pos.blockY(), pos.blockZ() + 0.5),
                    collidableBlock.collisionBox());
            if (hit) newBlocks.put(
                    new CollisionKey(collidableBlock, new BlockVec(pos)),
                    new CollidableBlock.Collision(world, this, pos, block)
            );
        }

        // Diff the new players with the old players
        for (var entry : newBlocks.entrySet()) {
            var removed = blocksTouching.remove(entry.getKey());
            if (removed == null && callEvents) {
                entry.getKey().block.onEnter(entry.getValue());
            }
        }
        for (var entry : blocksTouching.entrySet()) {
            if (callEvents) entry.getKey().block.onExit(entry.getValue());
        }
        blocksTouching.clear();
        blocksTouching.putAll(newBlocks);
    }

    private void updateTouchingMarkerEntities(@NotNull MapWorld world, boolean callEvents) {
        var position = getPosition();
        var minestomBoundingBox = getBoundingBox();
        var physicsBoundingBox = new net.hollowcube.mapmaker.map.util.spatial.BoundingBox(
                (float) (position.x() + minestomBoundingBox.minX()),
                (float) (position.y() + minestomBoundingBox.minY()),
                (float) (position.z() + minestomBoundingBox.minZ()),
                (float) (position.x() + minestomBoundingBox.maxX()),
                (float) (position.y() + minestomBoundingBox.maxY()),
                (float) (position.z() + minestomBoundingBox.maxZ())
        );

        var newObjects = world.collisionTree().intersectingObjects(physicsBoundingBox);
        for (var newObject : newObjects) {
            if (!objectsTouching.remove(newObject) && newObject instanceof Entity entity && callEvents) {
                world.callEvent(new Map2PlayerEnterEntityEvent(world, this, entity));
                if (entity instanceof MarkerEntity marker)
                    marker.onPlayerEntered2NoEventTemp(this);
            }
        }
        for (var object : objectsTouching) {
            if (object instanceof Entity entity && callEvents) {
                world.callEvent(new Map2PlayerExitEntityEvent(world, this, entity));
                if (entity instanceof MarkerEntity marker)
                    marker.onPlayerExited2NoEventTemp(this);
            }
        }
        objectsTouching.clear();
        objectsTouching.addAll(newObjects);
    }

    //endregion

    //region Tablist overrides to delegate to session manager

    @Override
    protected @NotNull PlayerInfoUpdatePacket getAddPlayerToList() {
        return new PlayerInfoUpdatePacket(EnumSet.of(PlayerInfoUpdatePacket.Action.ADD_PLAYER, PlayerInfoUpdatePacket.Action.UPDATE_LISTED), List.of(this.infoEntry()));
    }

    private PlayerInfoUpdatePacket.Entry infoEntry() {
        PlayerSkin skin = getSkin();
        List<PlayerInfoUpdatePacket.Property> prop = skin != null ? List.of(new PlayerInfoUpdatePacket.Property("textures", skin.textures(), skin.signature())) : List.of();
        // Listed is always false. SessionManager manages the tab list for us.
        return new PlayerInfoUpdatePacket.Entry(getUuid(), getUsername(), prop, false, getLatency(), getGameMode(), getDisplayName(), null, 0, true);
    }

    //endregion

    //region Viewable overrides for thread assertions

    @Override
    public boolean isAutoViewable() {
        FutureUtil.assertTickThreadWarn(acquirable());
        return super.isAutoViewable();
    }

    @Override
    public void setAutoViewable(boolean autoViewable) {
        FutureUtil.assertTickThreadWarn(acquirable());
        super.setAutoViewable(autoViewable);
    }

    @Override
    public void updateViewableRule(@Nullable Predicate<Player> predicate) {
        FutureUtil.assertTickThreadWarn(acquirable());
        super.updateViewableRule(predicate);
    }

    @Override
    public void updateViewableRule() {
        FutureUtil.assertTickThreadWarn(acquirable());
        super.updateViewableRule();
    }

    @Override
    public boolean autoViewEntities() {
        FutureUtil.assertTickThreadWarn(acquirable());
        return super.autoViewEntities();
    }

    @Override
    public void setAutoViewEntities(boolean autoViewer) {
        if (playerConnection.getConnectionState() == ConnectionState.PLAY)
            FutureUtil.assertTickThreadWarn(acquirable());
        super.setAutoViewEntities(autoViewer);
    }

    @Override
    public void updateViewerRule(@Nullable Predicate<Entity> predicate) {
        FutureUtil.assertTickThreadWarn(acquirable());
        super.updateViewerRule(predicate);
    }

    @Override
    public void updateViewerRule() {
        FutureUtil.assertTickThreadWarn(acquirable());
        super.updateViewerRule();
    }

    @Override
    public @NotNull Set<Player> getViewers() {
        // Left to make clear that it is excluded from tick thread warnings. The set only locks itself,
        // so should never be in a position to create a deadlock.
        return super.getViewers();
    }

    @Override
    public boolean hasPredictableViewers() {
        // Left to make clear that it is excluded from tick thread warnings. Only locks itself,
        // so should never be in a position to create a deadlock.
        return super.hasPredictableViewers();
    }

    @Override
    public @NotNull PlayerSnapshot updateSnapshot(@NotNull SnapshotUpdater updater) {
        FutureUtil.assertTickThreadWarn(acquirable());
        return super.updateSnapshot(updater);
    }

    //endregion

}
