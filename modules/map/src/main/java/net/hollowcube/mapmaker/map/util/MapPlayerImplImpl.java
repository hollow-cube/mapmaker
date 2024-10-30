package net.hollowcube.mapmaker.map.util;

import net.hollowcube.mapmaker.map.block.ghost.GhostBlockHolder;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.collision.PhysicsResult;
import net.minestom.server.collision.PhysicsUtils;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.metadata.LivingEntityMeta;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.player.PlayerConnection;
import net.minestom.server.utils.chunk.ChunkCache;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.UUID;

// I(matt)DK what to name this class lol
public abstract class MapPlayerImplImpl extends MapPlayerImpl implements PlayerRiptideExtension {
    private static final Logger logger = LoggerFactory.getLogger(MapPlayerImplImpl.class);

    private int riptideTicks = 0;

    // Only present sometimes (eg during riptide)
    private PhysicsResult nextPhysicsResult = null;

    public MapPlayerImplImpl(@NotNull UUID uuid, @NotNull String username, @NotNull PlayerConnection playerConnection) {
        super(uuid, username, playerConnection);
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
    public void tick(long time) {
        super.tick(time);

        logger.info("{}, start impl impl tick", getUsername());
        if (needsPhysicsPrediction()) {
            var velocity = Vec.fromPoint(position.sub(previousPosition));

            final Block.Getter chunkCache = new ChunkCache(instance, currentChunk, Block.STONE);
            nextPhysicsResult = PhysicsUtils.simulateMovement(position, velocity, boundingBox,
                    instance.getWorldBorder(), chunkCache, getAerodynamics(), hasNoGravity(), hasPhysics,
                    onGround, false, getLastPhysicsResult());
        } else {
            nextPhysicsResult = null;
        }
        logger.info("{} done physics", getUsername());

        if (riptideTicks > 0) {
            riptideTicks--;

            // Stop if we hit a wall
            if (nextPhysicsResult.collisionX() || nextPhysicsResult.collisionZ()) {
                riptideTicks = 0;
            }

            if (riptideTicks <= 0) {
                getPlayerMeta().setInRiptideSpinAttack(false);
            }
        }
        logger.info("{} done impl impl", getUsername());
    }

    // We override this to use our own canFitWithBoundingBox implementation which correctly handles per-player dripleaf states.
    @Override
    public void updatePose() {
        Pose oldPose = getPose();
        Pose newPose;

        // Figure out their expected state
        var meta = getEntityMeta();
        if (meta.isFlyingWithElytra()) {
            newPose = Pose.FALL_FLYING;
        } else if (false) { // When should they be sleeping? We don't have any in-bed state...
            newPose = Pose.SLEEPING;
        } else if (meta.isSwimming()) {
            newPose = Pose.SWIMMING;
        } else if (meta instanceof LivingEntityMeta livingMeta && livingMeta.isInRiptideSpinAttack()) {
            newPose = Pose.SPIN_ATTACK;
        } else if (isSneaking() && !isFlying()) {
            newPose = Pose.SNEAKING;
        } else {
            newPose = Pose.STANDING;
        }

        // Try to put them in their expected state, or the closest if they don't fit.
        if (canFitWithBoundingBox(newPose)) {
            // Use expected state
        } else if (canFitWithBoundingBox(Pose.SNEAKING)) {
            newPose = Pose.SNEAKING;
        } else if (canFitWithBoundingBox(Pose.SWIMMING)) {
            newPose = Pose.SWIMMING;
        } else {
            // If they can't fit anywhere, just use standing
            newPose = Pose.STANDING;
        }

        if (newPose != oldPose) setPose(newPose);
    }

    private boolean canFitWithBoundingBox(@NotNull Pose pose) {
        BoundingBox bb = pose == Pose.STANDING ? boundingBox : BoundingBox.fromPose(pose);
        if (bb == null) return false;

        var position = getPosition();
        var iter = bb.getBlocks(getPosition());
        while (iter.hasNext()) {
            var posMut = iter.next();
            var pos = new Vec(posMut.x(), posMut.y(), posMut.z());
            var block = instance.getBlock(pos, Block.Getter.Condition.TYPE);
            if (block.id() == Block.BIG_DRIPLEAF.id()) {
                // Fetch dripleaf state from the ghost block holder to make sure we get the right value for this player
                var ghostBlocks = GhostBlockHolder.forPlayerOptional(this);
                if (ghostBlocks != null) block = ghostBlocks.getBlock(pos);
            }

            // For now just ignore scaffolding. It seems to have a dynamic bounding box, or is just parsed
            // incorrectly in MinestomDataGenerator.
            if (block.id() == Block.SCAFFOLDING.id()) continue;

            var hit = block.registry().collisionShape()
                    .intersectBox(position.sub(pos.blockX(), pos.blockY(), pos.blockZ()), bb);
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
}
