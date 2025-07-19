package net.hollowcube.mapmaker.map.util;

import net.hollowcube.common.util.BlockUtil;
import net.hollowcube.mapmaker.map.block.ghost.GhostBlockHolder;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.collision.PhysicsResult;
import net.minestom.server.collision.PhysicsUtils;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityPose;
import net.minestom.server.entity.metadata.LivingEntityMeta;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import net.minestom.server.utils.chunk.ChunkCache;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Objects;

// I(matt)DK what to name this class lol
public abstract class MapPlayerImplImpl extends MapPlayerImpl implements PlayerRiptideExtension, PlayerLiquidExtension {
    private int riptideTicks = 0;

    // Only present sometimes (eg during riptide)
    private PhysicsResult nextPhysicsResult = null;

    private boolean isInWater, isInLava;

    public MapPlayerImplImpl(@NotNull PlayerConnection playerConnection, @NotNull GameProfile gameProfile) {
        super(playerConnection, gameProfile);
    }

    @Override
    public void beginRiptideAttack(int durationTicks) {
        this.riptideTicks = durationTicks;
    }

    @Override
    public void cancelRiptideAttack() {
        if (this.riptideTicks <= 0) return;

        this.riptideTicks = 0;
        getPlayerMeta().setInRiptideSpinAttack(false);
    }

    private boolean needsPhysicsPrediction() {
        return riptideTicks > 0;
    }

    @Override
    public boolean isInWater() {
        return isInWater;
    }

    @Override
    public boolean isInLava() {
        return isInLava;
    }

    @Override
    public void tick(long time) {
        super.tick(time);

        if (needsPhysicsPrediction()) {
            var velocity = Vec.fromPoint(position.sub(previousPosition));

            final Block.Getter chunkCache = new ChunkCache(instance, currentChunk, Block.STONE);
            nextPhysicsResult = PhysicsUtils.simulateMovement(position, velocity, boundingBox,
                    instance.getWorldBorder(), chunkCache, getAerodynamics(), hasNoGravity(), hasPhysics,
                    onGround, false, getLastPhysicsResult());
        } else {
            nextPhysicsResult = null;
        }

        if (riptideTicks > 0) {
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
    }

    // We override this to use our own canFitWithBoundingBox implementation which correctly handles per-player dripleaf states.
    @Override
    public void updatePose() {
        EntityPose oldPose = getPose();
        EntityPose newPose;

        // Figure out their expected state
        var meta = getEntityMeta();
        if (meta.isFlyingWithElytra()) {
            newPose = EntityPose.FALL_FLYING;
        } else if (false) { // When should they be sleeping? We don't have any in-bed state...
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
        updateWaterLavaState();
    }

    private void updateWaterLavaState() {
        final BoundingBox bb = getBoundingBox().contract(0.001, 0.001, 0.001);
        var position = getPosition();
        var instance = getInstance();

        isInWater = isInLava = false;
        var iter = bb.getBlocks(position);
        while (iter.hasNext()) {
            if (isInWater && isInLava) break;
            var posMut = iter.next();

            var block = instance.getBlock(posMut.blockX(), posMut.blockY(),
                    posMut.blockZ(), Block.Getter.Condition.TYPE);
            double fluidHeight = getFluidHeight(block);
            if (fluidHeight < 0) continue;

            var blockAbove = instance.getBlock(posMut.blockX(), posMut.blockY() + 1,
                    posMut.blockZ(), Block.Getter.Condition.TYPE);
            fluidHeight = block.id() == blockAbove.id() ? 1 : (fluidHeight / 9.0);
            if (posMut.blockY() + fluidHeight < position.y()) continue; // Not in fluid

            if (block.id() == Block.WATER.id() || BlockUtil.isWaterlogged(block)) {
                isInWater = true;
            } else if (block.id() == Block.LAVA.id()) {
                isInLava = true;
            }
        }
    }

    private static double getFluidHeight(@NotNull Block block) {
        var level = block.getProperty("level");
        if (level == null) return BlockUtil.isWaterlogged(block) ? 8 : -1;

        try {
            var height = Math.min(8, Double.parseDouble(level));
            return height == 0 ? 8 : 8 - height;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private boolean canFitWithBoundingBox(@NotNull EntityPose pose) {
        var instance = getInstance();
        if (instance == null) return true; // Sanity check not in an instance

        BoundingBox bb = pose == EntityPose.STANDING ? boundingBox : BoundingBox.fromPose(pose);
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

    private @NotNull PhysicsResult getLastPhysicsResult() {
        class Holder {
            static Field lastPhysicsResult = null;
        }

        try {
            if (Holder.lastPhysicsResult == null) {
                Holder.lastPhysicsResult = Entity.class.getDeclaredField("previousPhysicsResult");
                Holder.lastPhysicsResult.setAccessible(true);
            }

            return (PhysicsResult) Holder.lastPhysicsResult.get(this);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
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
}
