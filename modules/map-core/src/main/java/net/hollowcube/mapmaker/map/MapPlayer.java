package net.hollowcube.mapmaker.map;

import it.unimi.dsi.fastutil.ints.*;
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
import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.cosmetic.CosmeticType;
import net.hollowcube.mapmaker.map.block.BlockTags;
import net.hollowcube.mapmaker.map.block.CollidableBlock;
import net.hollowcube.mapmaker.map.block.ghost.GhostBlockHolder;
import net.hollowcube.mapmaker.map.command.DebugRenderersCommand;
import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.hollowcube.mapmaker.map.event.PlayerJumpEvent;
import net.hollowcube.mapmaker.map.event.entity.Map2PlayerEnterEntityEvent;
import net.hollowcube.mapmaker.map.event.entity.Map2PlayerExitEntityEvent;
import net.hollowcube.mapmaker.map.item.vanilla.FireworkRocketItem;
import net.hollowcube.mapmaker.map.util.PlayerVisibility;
import net.hollowcube.mapmaker.map.util.spatial.SpatialObject;
import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.player.DisplayName;
import net.hollowcube.mapmaker.player.PlayerData;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.MinecraftServer;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.collision.PhysicsResult;
import net.minestom.server.collision.PhysicsUtils;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.*;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.metadata.LivingEntityMeta;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.instance.Weather;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.ConnectionState;
import net.minestom.server.network.PlayerProvider;
import net.minestom.server.network.packet.client.common.ClientPongPacket;
import net.minestom.server.network.packet.server.SendablePacket;
import net.minestom.server.network.packet.server.common.PingPacket;
import net.minestom.server.network.packet.server.play.*;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.registry.RegistryTag;
import net.minestom.server.registry.TagKey;
import net.minestom.server.snapshot.PlayerSnapshot;
import net.minestom.server.snapshot.SnapshotUpdater;
import net.minestom.server.sound.SoundEvent;
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
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class MapPlayer extends CommandHandlingPlayer implements MiscFunctionality.CosmeticCallback {

    private static final Logger log = LoggerFactory.getLogger(MapPlayer.class);

    static {
        MinecraftServer.getPacketListenerManager().setPlayListener(ClientPongPacket.class, (packet, player) -> {
            if (player instanceof MapPlayer mp) mp.lastReceivedPingId = packet.id();
        });
        MinecraftServer.getGlobalEventHandler().addListener(PlayerMoveEvent.class, event -> {
            if (event.getPlayer() instanceof MapPlayer mp) {
                mp.handleMoveForFallDamage(event);
            }
        });
    }

    public static @NotNull PlayerProvider simpleMapPlayer(@NotNull CommandManager commandManager) {
        return (connection, profile) -> new MapPlayer(connection, profile) {
            @Override public @NotNull CommandManager getCommandManager() {
                return commandManager;
            }
        };
    }

    private static final IntSet RESET_FALL_INSIDE_BLOCKS = IntSet.of(
        // Bubble column, but only if its going up so its handled later.
        Block.COBWEB.id(), Block.POWDER_SNOW.id(), Block.SWEET_BERRY_BUSH.id(), Block.HONEY_BLOCK.id());
    private static final RegistryTag<@NotNull Block> TAG_CLIMBABLE = Block.staticRegistry()
        .getTag(TagKey.ofHash("#minecraft:climbable"));
    // TODO(1.21.11): this is an actual tag, use it.
    private static final IntSet CAN_GLIDE_THROUGH = IntSet.of(
        Block.VINE.id(), Block.TWISTING_VINES.id(), Block.TWISTING_VINES_PLANT.id(),
        Block.WEEPING_VINES.id(), Block.WEEPING_VINES_PLANT.id()
    );

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

    private double fallDistance = 0;
    private boolean extraParticlesOnLanding = false;
    private @Nullable Point impulsePosition = null;
    private boolean ignoreFallDamageFromImpulse = false;
    private int impulseGraceTicks = 0;

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

    @Override
    public void sendPacket(@NotNull SendablePacket packet) {
        switch (packet) {
            // In case it is sent from somewhere we dont expect
            case PingPacket(int id) -> lastPingId.set(id);

            // Don't send pose packets to self if we aren't supposed to
            case EntityMetaDataPacket(var entity, var entries)
                when entity == this.getEntityId() && entries.containsKey(MetadataDef.POSE.index()) && !canSendPose -> {
                var newEntries = new HashMap<>(entries);
                newEntries.remove(MetadataDef.POSE.index());
                super.sendPacket(new EntityMetaDataPacket(entity, newEntries));
                return;
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
        createNameTagEntity(player);
    }

    @Override
    public void updateOldViewer(@NotNull Player player) {
        FutureUtil.assertTickThreadWarn(acquirable());
        super.updateOldViewer(player);
        visibilityByEntity.remove(player.getEntityId());
        destroyNameTagEntity(player);
    }

    @Override
    public void sendPacketToViewers(@NotNull SendablePacket packet) {
        if (packet instanceof EntityMetaDataPacket metaPacket && interceptMetadataPacket(metaPacket))
            return; // Packet was handled by intercept function

        super.sendPacketToViewers(packet);
    }

    @Override
    public void update(long time) {
        super.update(time);

        physicsTick();
        cooldownTick();
        riptideTick();
        fallTick();
        weatherTick();
        jumpTick();
    }

    @Override
    public void remove(boolean permanent) {
        super.remove(permanent);
        visibilityByEntity.clear();
    }

    @Override
    public boolean setGameMode(@NotNull GameMode gameMode) {
        if (gameMode == GameMode.CREATIVE) resetImpulsePosition();
        return super.setGameMode(gameMode);
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

    @Override
    public void setPose(@NotNull EntityPose pose) {
        super.setPose(pose);

        updateNameTagForPose(pose);
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

    public int ping() {
        int id = lastPingId.incrementAndGet();
        sendPacket(new PingPacket(id));
        return id;
    }

    //endregion

    //region EXT: Latency

    private static final int LATENCY_SAMPLE_SIZE = 5;
    private final int[] latencySamples = new int[LATENCY_SAMPLE_SIZE];
    private int latencySampleIndex = 0;

    public double averageLatency() {
        long sum = 0;
        int count = 0;
        for (long sample : this.latencySamples) {
            if (sample <= 0) continue;
            sum += sample;
            count++;
        }
        return count > 0 ? (double) sum / (double) count : 0;
    }

    @Override
    public void refreshLatency(int latency) {
        this.latencySamples[this.latencySampleIndex] = latency;
        this.latencySampleIndex = (this.latencySampleIndex + 1) % LATENCY_SAMPLE_SIZE;

        super.refreshLatency(latency);
    }

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

        int cooldown = cooldownGroups.getInt(useCooldown.cooldownGroup());
        if (cooldown > 0) return false; // Still in cooldown

        int cooldownTicks = (int) (useCooldown.seconds() * 20);
        cooldownGroups.put(useCooldown.cooldownGroup(), cooldownTicks);
        sendPacket(new SetCooldownPacket(useCooldown.cooldownGroup(), cooldownTicks));
        return true;
    }

    private void cooldownTick() {
        // Tick cooldown
        for (Object2IntMap.Entry<String> cooldown : cooldownGroups.object2IntEntrySet()) {
            int newCooldown = cooldown.getIntValue() - 1;
            if (newCooldown <= 0) {
                cooldownGroups.removeInt(cooldown.getKey());
            } else {
                cooldown.setValue(newCooldown);
            }
        }
    }

    public void setItemCooldowns(Map<String, Integer> cooldowns) {
        cooldownGroups.putAll(cooldowns);
        for (Map.Entry<String, Integer> cooldown : cooldowns.entrySet()) {
            sendPacket(new SetCooldownPacket(cooldown.getKey(), cooldown.getValue()));
        }
    }

    public Map<String, Integer> getItemCooldowns() {
        return Map.copyOf(cooldownGroups);
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
        super.setFlyingWithElytra(isFlying && this.canFlyWithElytra());
        if (!isFlying) FireworkRocketItem.removeRocket(this);
    }

    public boolean canFlyWithElytra() {
        return this.getChestplate().has(DataComponents.GLIDER);
    }

    //endregion

    //region EXT: Fall Distance Tracking

    /// Returns the current fall distance for the player.
    public double fallDistance() {
        return fallDistance;
    }

    public void resetFallDistance() {
        fallDistance = 0;
    }

    public void trackImpulsePosition(Point position, boolean ignoreFallDamage) {
        this.impulsePosition = position;
        this.ignoreFallDamageFromImpulse = ignoreFallDamage;
        this.impulseGraceTicks = ignoreFallDamage ? 40 : 0;
    }

    public void resetImpulsePosition() {
        this.impulsePosition = null;
        this.ignoreFallDamageFromImpulse = false;
        this.impulseGraceTicks = 0;
    }

    private void handleMoveForFallDamage(PlayerMoveEvent event) {
        if (isInWater()) resetFallDistance();
        // todo need to check all the various conditions to not care about fall damage

        // TODO: need to deal with player checkFallDamage and livingEntity checkFallDamage
        var delta = event.getNewPosition().sub(getPosition());
        if (!isInWater() && delta.y() < 0)
            fallDistance -= delta.y();

        if (event.isOnGround() && fallDistance > 0) {
//            var blockFallingOn = getSupportingBlock(event.getNewPosition());
//            if (!blockFallingOn.isAir())
//                sendMessage("falling on " + blockFallingOn.name());
            // todo other block types deal different damage

            applyFallDamage(fallDistance, 1);
            resetFallDistance();
        } else if (event.isOnGround() && delta.y() > 0) {
            resetFallDistance();
        }

        boolean shouldResetImpulse = event.isOnGround() || isInClimbable() || getGameMode() == GameMode.SPECTATOR
                                     || getPose() == EntityPose.FALL_FLYING || getPose() == EntityPose.SPIN_ATTACK;
        if (!shouldResetImpulse && delta.y() < 1e-5) {
            updateBlockTouchState();
            shouldResetImpulse = isInWater() || isInLava();
        }
        if (shouldResetImpulse && impulseGraceTicks == 0)
            resetImpulsePosition();
    }

    private void fallTick() {
        if (isInLava()) fallDistance *= 0.5;

        if (impulseGraceTicks > 0) impulseGraceTicks--;

        if (hasEffect(PotionEffect.SLOW_FALLING) || hasEffect(PotionEffect.LEVITATION) || isFlying())
            resetFallDistance();
    }

    private void updateInsideBlocks() {
        var instance = getInstance();
        if (instance == null) return; // Sanity check not in an instance

        var bb = this.getBoundingBox();
        var iter = bb.getBlocks(getPosition());
        var blocks = Objects.requireNonNullElse(GhostBlockHolder.forPlayerOptional(this), instance);

        while (iter.hasNext()) {
            var posMut = iter.next();
            var pos = new Vec(posMut.x(), posMut.y(), posMut.z());
            var block = blocks.getBlock(pos, Block.Getter.Condition.TYPE);

            // This appears kind of cursed because we don't check collision shape, however
            // in vanilla only a few blocks have relevant entityInsideCollisionShapes, powdered
            // snow is the only relevant one to fall damage and that simply makes it solid if
            // the player is falling/able to walk. However the falling bit is only relevant
            // clientside (or when we process non-player entity fall damage). So we can
            // just treat everything as full block for now.

            boolean shouldResetFallDistance = RESET_FALL_INSIDE_BLOCKS.contains(block.id())
                                              || block.id() == Block.BUBBLE_COLUMN.id() && "false".equals(block.getProperty("drag"));
            if (shouldResetFallDistance) {
                resetFallDistance();
                if (impulseGraceTicks == 0) resetImpulsePosition();
                break; // only need to reset once
            }
        }

        if (isInClimbable()) resetFallDistance();
    }

    private void applyFallDamage(double fallDistance, float damageMultiplier) {
        if (isAllowFlying()) return;

        double effectiveFallDistance = fallDistance;
        if (impulsePosition != null && ignoreFallDamageFromImpulse) {
            effectiveFallDistance = Math.min(fallDistance, impulsePosition.y() - getPosition().y());
            if (effectiveFallDistance <= 0 || impulseGraceTicks == 0) {
                resetImpulsePosition();
            }
        }

        if (effectiveFallDistance <= 0) return;
        int damage = calculateFallDamage(effectiveFallDistance, damageMultiplier);
        if (damage <= 0) return;

        resetImpulsePosition();

        //todo need to send fall particles & extra fall particles also

        // Don't really need to send sounds right now since we dont play sounds generally for nearby players.
        // TODO: should only be playing for viewers anyway since the client predicts fall sounds
        // Player fall sound
//        playGlobalSound(damage > 4 ? SoundEvent.ENTITY_PLAYER_BIG_FALL : SoundEvent.ENTITY_PLAYER_SMALL_FALL);
        // Block fall sound
//        var block = instance.getBlock(getPosition().withY(y -> y - 0.2), Block.Getter.Condition.TYPE);
//        if (!block.isAir()) {
//            var blockSoundType = block.registry().getBlockSoundType();
//            if (blockSoundType != null) playGlobalSound(blockSoundType.fallSound(),
//                    blockSoundType.volume(), blockSoundType.pitch());
//        }

//        sendMessage("dealing " + damage + " damage...");
    }

    private int calculateFallDamage(double fallDistance, float damageMultiplier) {
        return (int) Math.floor(calculateFallPower(fallDistance)
                                * damageMultiplier
                                * getAttributeValue(Attribute.FALL_DAMAGE_MULTIPLIER));
    }

    private double calculateFallPower(double fallDistance) {
        double safeFallDistance = getAttributeValue(Attribute.SAFE_FALL_DISTANCE);
        // The max value is +1024, so we treat that as disabling fall damage entirely (server side only)
        if (safeFallDistance > 1023.0) return 0;

        return fallDistance + 1.0E-6 - safeFallDistance;
    }

    private boolean isInClimbable() {
        if (getGameMode() == GameMode.SPECTATOR || isFlying())
            return false;

        // Weirdly climbing specifically checks the block you are inside
        var insideBlock = instance.getBlock(getPosition(), Block.Getter.Condition.TYPE);
        if (getPose() == EntityPose.FALL_FLYING && CAN_GLIDE_THROUGH.contains(insideBlock.id()))
            return false;
        return TAG_CLIMBABLE.contains(insideBlock) || isTrapdoorAndClimbable(insideBlock, getPosition());
    }

    private boolean isTrapdoorAndClimbable(Block block, Point blockPosition) {
        if (!BlockTags.TRAPDOORS.contains(block.key()) || "true".equals(block.getProperty("open")))
            return false;
        var belowBlock = instance.getBlock(blockPosition.relative(BlockFace.BOTTOM), Block.Getter.Condition.TYPE);
        return belowBlock.id() == Block.LADDER.id() && Objects.equals(belowBlock.getProperty("facing"), block.getProperty("facing"));
    }

    private Block getSupportingBlock(Point selfPos) {
        var bb = getBoundingBox().grow(0, 1e-6, 0);

        Point closestPosition = null;
        double closestDistance = Double.MAX_VALUE;

        var iter = bb.getBlocks(selfPos);
        var blocks = Objects.requireNonNullElse(GhostBlockHolder.forPlayerOptional(this), instance);
        while (iter.hasNext()) {
            var posMut = iter.next();
            var pos = new Vec(posMut.x(), posMut.y(), posMut.z());
            var block = blocks.getBlock(pos, Block.Getter.Condition.TYPE);

            if (posMut.y() >= selfPos.y()) continue;

            var hit = block.registry().collisionShape().intersectBox(
                selfPos.sub(pos.blockX(), pos.blockY(), pos.blockZ()), bb);
            if (hit) {
                double distance = pos.add(0.5).distanceSquared(selfPos);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestPosition = pos;
                }
            }
        }

        if (closestPosition == null) return Block.AIR;
        return blocks.getBlock(closestPosition, Block.Getter.Condition.TYPE);
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

            updateInsideBlocks();
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

    //region EXT: Weather

    private float targetRainLevel = 0, targetThunderLevel = 0;
    private float currentRainLevel = 0, currentThunderLevel = 0;
    private float weatherTransitionRate = 0.04f; //todo get the vanilla value

    public void setWeather(Weather weather) {
        this.targetRainLevel = weather.rainLevel();
        this.targetThunderLevel = weather.thunderLevel();
    }

    public void setWeather(Weather weather, float transitionRate) {
        this.targetRainLevel = weather.rainLevel();
        this.targetThunderLevel = weather.thunderLevel();
        this.weatherTransitionRate = Math.abs(transitionRate);
    }

    private void weatherTick() {
        if (currentRainLevel == targetRainLevel && currentThunderLevel == targetThunderLevel)
            return; // Nothing to do

        float lastRainLevel = currentRainLevel, lastThunderLevel = currentThunderLevel;
        currentRainLevel = Math.clamp(currentRainLevel + Math.copySign(weatherTransitionRate, targetRainLevel - currentRainLevel), 0, 1);
        currentThunderLevel = Math.clamp(currentThunderLevel + Math.copySign(weatherTransitionRate, targetThunderLevel - currentThunderLevel), 0, 1);

        if (currentRainLevel > 0 && lastRainLevel == 0) {
            sendPacket(new ChangeGameStatePacket(ChangeGameStatePacket.Reason.BEGIN_RAINING, 0));
        }

        if (lastRainLevel != currentRainLevel)
            sendPacket(new ChangeGameStatePacket(ChangeGameStatePacket.Reason.RAIN_LEVEL_CHANGE, currentRainLevel));
        if (lastThunderLevel != currentThunderLevel)
            sendPacket(new ChangeGameStatePacket(ChangeGameStatePacket.Reason.THUNDER_LEVEL_CHANGE, currentThunderLevel));

        if (currentRainLevel == 0 && lastRainLevel != 0) {
            sendPacket(new ChangeGameStatePacket(ChangeGameStatePacket.Reason.END_RAINING, 0));
        }
    }

    //endregion

    //region EXT: Jump Event

    private boolean wasJumping = false;

    private void jumpTick() {
        if (inputs().jump() && !wasJumping) {
            wasJumping = true;
            EventDispatcher.call(new PlayerJumpEvent(this)); // todo: probably should just be part of minestom
        }
    }

    @Override
    public void refreshInput(boolean forward, boolean backward, boolean left, boolean right, boolean jump, boolean shift, boolean sprint) {
        super.refreshInput(forward, backward, left, right, jump, shift, sprint);
        jumpTick();
    }

    @Override
    public void refreshOnGround(boolean onGround) {
        super.refreshOnGround(onGround);
        if (onGround) wasJumping = false;
    }

    //endregion

    //region EXT: Name Tag

    private static final byte META_FLAG_INVISIBLE = (byte) 0b100000;
    private static final byte META_FLAG_SNEAKING = (byte) 0b10;

    private final int nameTagEntityId = Entity.generateId(), nameTagOffsetEntityId = Entity.generateId();
    private final UUID nameTagEntityUuid = UUID.randomUUID(), nameTagOffsetEntityUuid = UUID.randomUUID();

    private void createNameTagEntity(Player player) {
        var playerData = PlayerData.fromPlayer(this);
        var displayName = playerData.displayName2().build(DisplayName.Context.NAME_TAG);
        player.sendPackets(List.of(
            new BundlePacket(),
            new SpawnEntityPacket(nameTagEntityId, nameTagEntityUuid, EntityType.ARMOR_STAND,
                getPosition(), 0, 0, Vec.ZERO),
            new EntityMetaDataPacket(nameTagEntityId, Map.of(
                MetadataDef.ENTITY_FLAGS.index(), Metadata.Byte(META_FLAG_INVISIBLE),
                MetadataDef.CUSTOM_NAME.index(), Metadata.OptComponent(displayName),
                MetadataDef.CUSTOM_NAME_VISIBLE.index(), Metadata.Boolean(true),
                MetadataDef.ArmorStand.ARMOR_STAND_FLAGS.index(), Metadata.Byte((byte) 0x10) // marker
            )),
            new SpawnEntityPacket(nameTagOffsetEntityId, nameTagOffsetEntityUuid, EntityType.INTERACTION,
                getPosition(), 0, 0, Vec.ZERO),
            new EntityMetaDataPacket(nameTagOffsetEntityId, Map.of(
                MetadataDef.ENTITY_FLAGS.index(), Metadata.Byte(META_FLAG_INVISIBLE),
                MetadataDef.Interaction.WIDTH.index(), Metadata.Float(0f),
                MetadataDef.Interaction.HEIGHT.index(), Metadata.Float(0f)
            )),
            new SetPassengersPacket(getEntityId(), List.of(nameTagOffsetEntityId)),
            new SetPassengersPacket(nameTagOffsetEntityId, List.of(nameTagEntityId)),
            new BundlePacket()
        ));
    }

    private void destroyNameTagEntity(Player player) {
        player.sendPacket(new DestroyEntitiesPacket(nameTagEntityId));
    }

    private void updateNameTagForPose(@NotNull EntityPose pose) {
        byte flag = pose == EntityPose.SNEAKING ? (META_FLAG_INVISIBLE | META_FLAG_SNEAKING) : META_FLAG_INVISIBLE;
        sendPacketToViewers(new EntityMetaDataPacket(nameTagEntityId, Map.of(
            MetadataDef.ENTITY_FLAGS.index(), Metadata.Byte(flag)
        )));
    }

    @Override
    public void onCosmeticChange(@NotNull CosmeticType type, @Nullable Cosmetic cosmetic) {
        if (type != CosmeticType.HAT) return;

        sendPacketToViewers(new EntityMetaDataPacket(nameTagOffsetEntityId, Map.of(
            MetadataDef.Interaction.HEIGHT.index(), Metadata.Float(cosmetic != null ? 0.2f : 0f)
        )));
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
        if (playerConnection.getServerState() == ConnectionState.PLAY)
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

    private void playGlobalSound(SoundEvent soundEvent) {
        playGlobalSound(soundEvent, 1, 1);
    }

    private void playGlobalSound(SoundEvent soundEvent, float volume, float pitch) {
        long seed = ThreadLocalRandom.current().nextLong();
        sendPacketToViewersAndSelf(new SoundEffectPacket(
            soundEvent, Sound.Source.PLAYER, getPosition(),
            volume, pitch, seed
        ));
    }

}
