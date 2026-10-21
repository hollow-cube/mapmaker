package net.hollowcube.mapmaker.map.entity;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.hollowcube.mapmaker.map.util.NbtUtil;
import net.hollowcube.mapmaker.map.util.datafix.legacy.PreDataFixFixes;
import net.hollowcube.mapmaker.util.ProtocolUtil;
import net.hollowcube.terraform.legacy.entity.TerraformEntity;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minestom.server.ServerFlag;
import net.minestom.server.collision.CollisionUtils;
import net.minestom.server.collision.PhysicsResult;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.*;
import net.minestom.server.entity.metadata.EntityMeta;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.play.EntityPositionAndRotationPacket;
import net.minestom.server.network.packet.server.play.EntityPositionPacket;
import net.minestom.server.network.packet.server.play.EntityPositionSyncPacket;
import net.minestom.server.network.packet.server.play.EntityRotationPacket;
import net.minestom.server.network.packet.server.play.data.PositionPath;
import net.minestom.server.network.packet.server.play.data.VecDelta;
import net.minestom.server.property.ServerProperties;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.utils.UUIDUtils;
import net.minestom.server.utils.chunk.ChunkCache;
import net.minestom.server.utils.chunk.ChunkUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static net.hollowcube.common.math.MathUtil.quantize4096;
import static net.hollowcube.common.util.BlockUtil.blockRestitution;
import static net.hollowcube.common.util.BlockUtil.suppressesBounce;

@SuppressWarnings("UnstableApiUsage")
public class MapEntity<M extends EntityMeta> extends Entity implements TerraformEntity {

    public static final MapEntityInfo<@NotNull MapEntity<? extends EntityMeta>> INFO = MapEntityInfo.builder()
        .with("Silent", MapEntityInfoType.Bool(false, EntityMeta::setSilent, EntityMeta::isSilent))
        .build();


    private static final String SILENT_KEY = "Silent";

    private static final int STEP_FLUSH_TICKS = 3;

    private PhysicsResult previousPhysicsResult;

    private boolean steppedSync = false;
    private final List<PositionPath.Step> pendingSteps = new ArrayList<>();
    private Pos flushedPosition = Pos.ZERO;
    private int ticksSinceStep = 0;
    private int ticksSinceFlush = 0;
    private boolean absoluteFlush = false;
    private boolean stepOnGround = false;
    private boolean flushedOnGround = false;
    private boolean reportedOnGround = false;

    protected MapEntity(@NotNull EntityType entityType) {
        super(entityType, UUID.randomUUID());
    }

    protected MapEntity(@NotNull EntityType entityType, @NotNull UUID uuid) {
        super(entityType, uuid);
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull M getEntityMeta() {
        return (M) super.getEntityMeta();
    }

    // Interaction

    /**
     * Called when a player interacts (right click) with this entity in build OR test/play/verify mode.
     * Note that this function is never called for spectating players.
     *
     * <p>This should ONLY be used to handle entities which need interaction in a playing state. If the entity
     * only needs to be configured in build mode then {@link #onBuildRightClick(MapWorld, Player, PlayerHand, Point)}
     * should be used.</p>
     */
    public void onRightClick(@NotNull MapWorld world, @NotNull Player player,
                             @NotNull PlayerHand hand, @NotNull Point interactPosition) {
        if (!world.canEdit(player)) return;
        onBuildRightClick(world, player, hand, interactPosition);
    }

    /**
     * Called when a player interacts (right click) with this entity in build mode.
     *
     * <p>Note: this function is called from {@link #onRightClick(MapWorld, Player, PlayerHand, Point)},
     * so may not be called if that is overridden.</p>
     */
    public void onBuildRightClick(@NotNull MapWorld world, @NotNull Player player,
                                  @NotNull PlayerHand hand, @NotNull Point interactPosition) {
        // No interaction by default
    }

    /**
     * Called when a player left-clicks this entity in build OR test/play/verify mode.
     * Note that this function is never called for spectating players.
     *
     * <p>This should ONLY be used to handle entities which need interaction in a playing state. If the entity
     * only needs to be configured in build mode then {@link #onBuildLeftClick(MapWorld, Player)} should be used.</p>
     */
    public void onLeftClick(@NotNull MapWorld world, @NotNull Player player) {
        if (!world.canEdit(player)) return;
        onBuildLeftClick(world, player);
    }

    /**
     * Called when a player left-clicks this entity in build mode.
     *
     * <p>Note: this function is called from {@link #onLeftClick(MapWorld, Player)},
     * so may not be called if that is overridden.</p>
     */
    public void onBuildLeftClick(@NotNull MapWorld world, @NotNull Player player) {
        // No interaction by default
    }

    // Misc utilities

    protected @NotNull Sound.Source soundSource() {
        return Sound.Source.NEUTRAL;
    }

    protected void playSound(@NotNull SoundEvent event, float volume, float pitch) {
        var position = getPosition();
        getViewersAsAudience().playSound(Sound.sound(event, soundSource(), volume, pitch),
                position.x(), position.y(), position.z());
    }

    @Override
    public void update(long time) {
        super.update(time);

        if (this.position.y() < -2032.0) {
            this.remove();
        }
    }

    // Physics

    protected double bounciness() {
        return 0.0;
    }

    protected double frictionModifier() {
        return 1.0;
    }

    protected double airDragModifier() {
        return 1.0;
    }

    /// Make vertical air drag use the horizontal base, so bounces decay symmetrically.
    protected boolean omnidirectionalAirDrag() {
        return false;
    }

    private boolean hasDefaultPhysics() {
        return bounciness() == 0.0
               && frictionModifier() == 1.0
               && airDragModifier() == 1.0
               && !omnidirectionalAirDrag();
    }

    protected static double computeModifiedFriction(double friction, double modifier) {
        return Math.clamp(1.0 - (1.0 - friction) * modifier, 0.0, 1.0);
    }

    @Override
    protected void movementTick() {
        // Legacy behavior to avoid potential regressions, probably can drop this later.
        if (!hasPhysics || hasDefaultPhysics()) {
            setSteppedSync(false);
            super.movementTick();
            return;
        }
        setSteppedSync(true);

        this.gravityTickCount = onGround ? 0 : gravityTickCount + 1;
        if (vehicle != null) return;

        var velocityPerTick = velocity.div(ServerProperties.SERVER_TICKS_PER_SECOND.get());
        var chunkCache = new ChunkCache(instance, currentChunk, Block.STONE);
        var result = CollisionUtils.handlePhysics(
                chunkCache, instance.getWorldBorder(), boundingBox, position, velocityPerTick, previousPhysicsResult, false);
        this.previousPhysicsResult = result;

        var finalChunk = ChunkUtils.retrieve(instance, currentChunk, result.newPosition());
        if (!ChunkUtils.isLoaded(finalChunk)) return;

        var newPosition = result.newPosition();
        onGround = result.isOnGround();

        var newVelocity = applyRestitution(chunkCache, result, velocityPerTick, newPosition);
        var bounced = (result.collisionX() && newVelocity.x() != 0)
            || (result.collisionY() && newVelocity.y() != 0)
            || (result.collisionZ() && newVelocity.z() != 0);
        newVelocity = applyPhysicsGravityAndDrag(chunkCache, result, newPosition, newVelocity);

        velocity = newVelocity.mul(ServerProperties.SERVER_TICKS_PER_SECOND.get());
        refreshPosition(newPosition, true, false);
        recordStep();
        if (bounced && hasVelocity()) sendPacketToViewers(getVelocityPacket());
    }

    private void setSteppedSync(boolean stepped) {
        if (steppedSync == stepped) return;
        steppedSync = stepped;
        setSynchronizationTicks(stepped ? Integer.MAX_VALUE : ServerProperties.ENTITY_SYNCHRONIZATION_TICKS.get());
        if (stepped) {
            // Minestom's truncated linear deltas leave the client somewhere near lastSyncedPosition, so
            // the first flush has to be absolute.
            flushedPosition = position;
            absoluteFlush = true;
        } else {
            synchronizeNextTick();
        }
    }

    private void recordStep() {
        // refreshPosition skips any position equal to lastSyncedPosition, so it must track every step
        // rather than only the flushed ones.
        lastSyncedPosition = position;
        ticksSinceStep++;
        var previous = pendingSteps.isEmpty() ? flushedPosition : pendingSteps.getLast().position();
        if (!position.samePoint(previous)) {
            pendingSteps.add(new PositionPath.Step(position, ticksSinceStep));
            ticksSinceStep = 0;
        }

        // A landing ends the batch early so that the next flush, STEP_FLUSH_TICKS later, arrives exactly
        // as the client reaches the floor.
        boolean landed = onGround && !stepOnGround;
        stepOnGround = onGround;
        if (++ticksSinceFlush < STEP_FLUSH_TICKS && !landed) return;
        ticksSinceFlush = 0;
        // Resets this on send so a step after a long rest is not stretched.
        ticksSinceStep = 0;
        if (!pendingSteps.isEmpty()) {
            flushSteps();
            return;
        }
        if (flushedOnGround != reportedOnGround) {
            // Came to rest on the landing, so no move will carry the ground state.
            sendPacketToViewers(new EntityRotationPacket(getEntityId(), position.yaw(), position.pitch(), flushedOnGround));
            reportedOnGround = flushedOnGround;
        }
        flushedOnGround = onGround;
    }

    private void flushSteps() {
        // The client applies onGround on receipt (landing particles and squish) but only reaches the end
        // of the previous batch by then, so report the ground state from there rather than the current one.
        reportedOnGround = flushedOnGround;
        var delta = absoluteFlush ? null : relativeSteps();
        if (delta == null) {
            sendPacketToViewers(new EntityPositionSyncPacket(getEntityId(), new PositionPath.Stepped(pendingSteps),
                position.yaw(), position.pitch(), reportedOnGround));
        } else if (position.sameView(flushedPosition)) {
            sendPacketToViewers(new EntityPositionPacket(getEntityId(), delta, reportedOnGround));
        } else {
            sendPacketToViewers(new EntityPositionAndRotationPacket(getEntityId(), delta, position.yaw(), position.pitch(), reportedOnGround));
        }
        flushedPosition = position;
        flushedOnGround = onGround;
        pendingSteps.clear();
        absoluteFlush = false;
    }

    private VecDelta.@Nullable Stepped relativeSteps() {
        var steps = new ArrayList<VecDelta.Step>(pendingSteps.size());
        Point previous = flushedPosition;
        for (var step : pendingSteps) {
            long dx = quantize4096(step.position().x()) - quantize4096(previous.x());
            long dy = quantize4096(step.position().y()) - quantize4096(previous.y());
            long dz = quantize4096(step.position().z()) - quantize4096(previous.z());
            if (dx != (short) dx || dy != (short) dy || dz != (short) dz) return null;
            steps.add(new VecDelta.Step(step.tickOffset(), (short) dx, (short) dy, (short) dz));
            previous = step.position();
        }
        return new VecDelta.Stepped(steps);
    }

    @Override
    protected void synchronizePosition() {
        pendingSteps.clear();
        ticksSinceStep = 0;
        super.synchronizePosition();
        flushedPosition = position;
    }

    @Override
    public void updateNewViewer(@NotNull Player player) {
        super.updateNewViewer(player);
        // The new viewer spawns at the current position, not at the base the pending deltas are relative to.
        if (!pendingSteps.isEmpty()) absoluteFlush = true;
    }

    private @NotNull Vec applyRestitution(@NotNull Block.Getter instance, @NotNull PhysicsResult result,
                                          @NotNull Vec velocityPerTick, @NotNull Pos newPosition) {
        var newVelocity = result.newVelocity();
        if (!result.hasCollision()) return newVelocity;

        var restitution = bounciness();
        if (result.collisionX()) newVelocity = newVelocity.withX(-velocityPerTick.x() * restitution);
        if (result.collisionZ()) newVelocity = newVelocity.withZ(-velocityPerTick.z() * restitution);
        if (result.collisionY() && velocityPerTick.y() != 0) {
            var gravity = hasNoGravity() ? 0.0 : getAerodynamics().gravity();
            var restitutionY = restitution;
            if (velocityPerTick.y() < 0) { // floor: the block's own restitution can exceed the entity's
                var below = blockBelow(instance, newPosition);
                // The one-gravity-tick fall speed cutoff is what stops infinite micro-bouncing at rest.
                restitutionY = below != null && -velocityPerTick.y() >= gravity && !suppressesBounce(below)
                    ? Math.max(restitution, blockRestitution(below)) : 0.0;
            }
            if (restitutionY > 0) {
                // Pre-compensate the gravity and drag applied later this tick, scaled by the
                // portion of the tick spent moving before the impact; without this every hop
                // loses a full gravity tick and the bounce decays too fast.
                var portion = (newPosition.y() - position.y()) / velocityPerTick.y();
                var gravityCompensation = portion * gravity;
                var aerodynamics = getAerodynamics();
                var airDrag = computeModifiedFriction(omnidirectionalAirDrag()
                    ? aerodynamics.horizontalAirResistance() : aerodynamics.verticalAirResistance(), airDragModifier());
                var effectiveDrag = 1.0 + (airDrag - 1.0) * portion;
                newVelocity = newVelocity.withY((gravityCompensation - velocityPerTick.y()) * effectiveDrag * restitutionY);
            }
        }
        return newVelocity;
    }

    protected @NotNull Vec applyPhysicsGravityAndDrag(@NotNull Block.Getter blockGetter, @NotNull PhysicsResult result,
                                                      @NotNull Pos newPosition, @NotNull Vec velocityPerTick) {
        var aerodynamics = getAerodynamics();
        var airDrag = computeModifiedFriction(aerodynamics.horizontalAirResistance(), airDragModifier());

        var groundFriction = 1.0;
        if (onGround) {
            final Block below = blockBelow(blockGetter, newPosition);
            if (below != null)
                groundFriction = computeModifiedFriction(below.friction(), frictionModifier());
        }

        var verticalDrag = omnidirectionalAirDrag() ? airDrag
            : computeModifiedFriction(aerodynamics.verticalAirResistance(), airDragModifier());
        var horizontal = groundFriction * airDrag;

        var x = velocityPerTick.x() * horizontal;
        var y = hasNoGravity() ? velocityPerTick.y() : (velocityPerTick.y() - aerodynamics.gravity()) * verticalDrag;
        var z = velocityPerTick.z() * horizontal;
        return new Vec(
            Math.abs(x) < Vec.EPSILON ? 0 : x,
            Math.abs(y) < Vec.EPSILON ? 0 : y,
            Math.abs(z) < Vec.EPSILON ? 0 : z);
    }

    private static @UnknownNullability Block blockBelow(@NotNull Block.Getter blockGetter, @NotNull Point position) {
        return blockGetter.getBlock(position.sub(0, 0.5 + Vec.EPSILON, 0), Block.Getter.Condition.TYPE);
    }

    // Serialization

    @Override
    public void readData(@NotNull CompoundBinaryTag tag) {
        // TODO these nbt fields are the same as vanilla, will need to be changed so data fixers work properly.
        var keys = tag.keySet();

        var meta = getEntityMeta();
        if (keys.contains("CustomName")) {
            var name = GsonComponentSerializer.gson().deserialize(tag.getString("CustomName"));
            meta.setCustomName(name);
        }
        meta.setCustomNameVisible(tag.getBoolean("CustomNameVisible", false));
        meta.setHasGlowingEffect(tag.getBoolean("Glowing", false));
        meta.setOnFire(tag.getBoolean("HasVisualFire", false));
        if (tag.getBoolean("NoGravity", false)) {
            setNoGravity(true);
            hasPhysics = false;
        } else {
            setNoGravity(false);
            hasPhysics = true;
        }

        // Vanilla
        this.getEntityMeta().setSilent(tag.getBoolean(SILENT_KEY, false));
    }

    @Override
    public void writeData(@NotNull CompoundBinaryTag.Builder tag) {
        // TODO these nbt fields are the same as vanilla, will need to be changed so data fixers work properly.
        tag.putString("id", getEntityType().name());
        tag.put("uuid", UUIDUtils.toNbt(getUuid()));
        tag.put("Pos", NbtUtil.into(getPosition()));
        tag.put("Rotation", NbtUtil.writeRotation(getPosition()));

        var meta = getEntityMeta();
        if (meta.getCustomName() != null) {
            var name = GsonComponentSerializer.gson().serialize(meta.getCustomName());
            tag.putString("CustomName", name);
        }
        if (meta.isCustomNameVisible()) tag.putBoolean("CustomNameVisible", true);
        if (meta.isHasGlowingEffect()) tag.putBoolean("Glowing", true);
        if (meta.isOnFire()) tag.putBoolean("HasVisualFire", true);
        if (hasNoGravity()) tag.putBoolean("NoGravity", true);

        // Vanilla
        tag.putBoolean(SILENT_KEY, this.getEntityMeta().isSilent());
    }

    @Deprecated // Should never be used, but cannot be removed for backwards compatibility.
    public void legacyLoad(@NotNull NetworkBuffer buffer, int version) {
        // Read the metadata
        var metadata = EntityMetadataStealer.steal(this);
        var loadedMetadata = ProtocolUtil.readMap(buffer, NetworkBuffer.VAR_INT, b1 -> {
            int type = PreDataFixFixes.fixEntityMetaIndex1_20_4(b1.read(NetworkBuffer.VAR_INT));
            return PreDataFixFixes.readEntityMeta1_20_4(type, b1);
        });
        for (var entry : loadedMetadata.entrySet()) {
            MetadataDef.Entry<Object> defEntry = new MetadataDef.Entry.Index<>(
                    entry.getKey(), t -> {
                return (Metadata.Entry<Object>) entry.getValue();
            }, entry.getValue());
            metadata.set(defEntry, entry.getValue());
        }

        // Read the nbt (can be end tag with no data)
        if (buffer.read(NetworkBuffer.NBT) instanceof CompoundBinaryTag compound) {
            tagHandler().updateContent(compound);
        }
    }
}
