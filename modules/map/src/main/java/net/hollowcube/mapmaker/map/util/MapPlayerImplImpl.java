package net.hollowcube.mapmaker.map.util;

import net.hollowcube.mapmaker.map.block.ghost.GhostBlockHolder;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.entity.metadata.LivingEntityMeta;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.player.PlayerConnection;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

// I(matt)DK what to name this class lol
public abstract class MapPlayerImplImpl extends MapPlayerImpl {

    public MapPlayerImplImpl(@NotNull UUID uuid, @NotNull String username, @NotNull PlayerConnection playerConnection) {
        super(uuid, username, playerConnection);
    }

    // We override this to use our own canFitWithBoundingBox implementation which correctly handles per-player dripleaf states.
    @Override
    protected void updatePose() {
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
            var pos = iter.next();
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
}
