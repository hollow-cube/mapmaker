package net.hollowcube.common.extensions;

import net.hollowcube.common.util.BlockUtil;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.entity.EntityPose;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

// Extension of Player that handles certain things minestom doesnt.
// TODO handle setting of swimming
public class ExtendedPlayer extends Player {

    private boolean isInWater = false;
    private boolean isInLava = false;

    public ExtendedPlayer(@NotNull PlayerConnection connection, @NotNull GameProfile profile) {
        super(connection, profile);
    }

    @Override
    public void update(long time) {
        super.update(time);

        updateBlockTouchState();
    }

    //region EXT: Touching Blocks

    public boolean isInWater() {
        return isInWater;
    }

    public boolean isInLava() {
        return isInLava;
    }

    protected void updateBlockTouchState() {
        final BoundingBox bb = getBoundingBox().contract(0.002, 0.002, 0.002);
        var position = getPosition();
        var instance = getInstance();
        if (instance == null) return;

        var meta = this.getPlayerMeta();

        isInWater = isInLava = false;
        boolean isInPowderedSnow = false;

        var iter = bb.getBlocks(position);
        while (iter.hasNext()) {
            if (isInWater && isInLava && isInPowderedSnow) break;
            var posMut = iter.next();

            var block = instance.getBlock(
                    posMut.blockX(), posMut.blockY(), posMut.blockZ(),
                    Block.Getter.Condition.TYPE);
            if (block == null) continue;
            double fluidHeight = getFluidHeight(block);
            if (fluidHeight >= 0) {
                var blockAbove = instance.getBlock(posMut.blockX(), posMut.blockY() + 1,
                                                   posMut.blockZ(), Block.Getter.Condition.TYPE);
                fluidHeight = blockAbove != null && block.id() == blockAbove.id() ? 1 : (fluidHeight / 9.0);
                if (posMut.blockY() + fluidHeight < position.y()) continue; // Not in fluid

                if (block.id() == Block.WATER.id() || BlockUtil.isWaterlogged(block)) {
                    isInWater = true;
                } else if (block.id() == Block.LAVA.id()) {
                    isInLava = true;
                }
            } else if (block.id() == Block.POWDER_SNOW.id()) {
                isInPowderedSnow = true;
                var ticks = meta.getTickFrozen();
                if (ticks < 140) {
                    meta.setTickFrozen(Math.min(ticks + 1, 140));
                }
            }
        }

        if (!isInPowderedSnow) {
            var ticks = meta.getTickFrozen();
            if (ticks > 0) {
                meta.setTickFrozen(Math.max(ticks - 2, 0));
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

    //endregion

    // region Bounding Box Scaling
    // Notes: This does not handle all bounding boxes due to some of them just not handling poses anyway.

    @Override
    public @NotNull BoundingBox getBoundingBox() {
        return this.getBoundingBox(this.getPose());
    }

    public BoundingBox getBoundingBox(@NotNull EntityPose pose) {
        if (pose == EntityPose.SLEEPING) return BoundingBox.fromPose(pose); // Vanilla doesnt scale sleeping box
        var box = Objects.requireNonNullElse(BoundingBox.fromPose(pose), this.boundingBox);
        var scale = this.getAttributeValue(Attribute.SCALE);
        return scale == 1.0 ? box : new BoundingBox(box.width() * scale, box.height() * scale, box.depth() * scale);
    }
    // endregion
}
