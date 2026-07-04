package net.hollowcube.mapmaker.map.entity;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.hollowcube.mapmaker.map.util.NbtUtil;
import net.hollowcube.mapmaker.map.util.datafix.legacy.PreDataFixFixes;
import net.hollowcube.mapmaker.util.ProtocolUtil;
import net.hollowcube.terraform.entity.TerraformEntity;
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
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.utils.UUIDUtils;
import net.minestom.server.utils.chunk.ChunkCache;
import net.minestom.server.utils.chunk.ChunkUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.util.UUID;

import static net.hollowcube.common.util.BlockUtil.blockRestitution;
import static net.hollowcube.common.util.BlockUtil.suppressesBounce;

@SuppressWarnings("UnstableApiUsage")
public class MapEntity<M extends EntityMeta> extends Entity implements TerraformEntity {

    public static final MapEntityInfo<@NotNull MapEntity<? extends EntityMeta>> INFO = MapEntityInfo.builder()
        .with("Silent", MapEntityInfoType.Bool(false, EntityMeta::setSilent, EntityMeta::isSilent))
        .build();


    private static final String SILENT_KEY = "Silent";

    private PhysicsResult previousPhysicsResult;

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
            super.movementTick();
            return;
        }

        this.gravityTickCount = onGround ? 0 : gravityTickCount + 1;
        if (vehicle != null) return;

        var velocityPerTick = velocity.div(ServerFlag.SERVER_TICKS_PER_SECOND);
        var chunkCache = new ChunkCache(instance, currentChunk, Block.STONE);
        var result = CollisionUtils.handlePhysics(
                chunkCache, boundingBox, position, velocityPerTick, previousPhysicsResult, false);
        this.previousPhysicsResult = result;

        var finalChunk = ChunkUtils.retrieve(instance, currentChunk, result.newPosition());
        if (!ChunkUtils.isLoaded(finalChunk)) return;

        var newPosition = CollisionUtils.applyWorldBorder(instance.getWorldBorder(), position, result.newPosition());
        onGround = result.isOnGround();

        var newVelocity = applyRestitution(chunkCache, result, velocityPerTick, newPosition);
        var bounced = (result.collisionX() && newVelocity.x() != 0)
            || (result.collisionY() && newVelocity.y() != 0)
            || (result.collisionZ() && newVelocity.z() != 0);
        newVelocity = applyPhysicsGravityAndDrag(chunkCache, result, newPosition, newVelocity);

        velocity = newVelocity.mul(ServerFlag.SERVER_TICKS_PER_SECOND);
        refreshPosition(newPosition, true, true);
        if (bounced && hasVelocity()) sendPacketToViewers(getVelocityPacket());
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
                groundFriction = computeModifiedFriction(below.registry().friction(), frictionModifier());
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
