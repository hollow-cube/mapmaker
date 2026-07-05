package net.hollowcube.common.util;

import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

public final class FluidUtil {

    public record Result(boolean inWater, boolean inLava, boolean inPowderSnow,
                         double waterHeight, double lavaHeight) {
    }

    public static @NotNull Result scan(@NotNull Block.Getter blockGetter, @NotNull BoundingBox boundingBox,
                                       @NotNull Point position) {
        final BoundingBox bb = boundingBox.contract(0.002, 0.002, 0.002);

        boolean inWater = false, inLava = false, inPowderSnow = false;
        double waterHeight = 0, lavaHeight = 0;

        var iter = bb.getBlocks(position);
        while (iter.hasNext()) {
            var posMut = iter.next();

            var block = blockGetter.getBlock(
                    posMut.blockX(), posMut.blockY(), posMut.blockZ(),
                    Block.Getter.Condition.TYPE);
            if (block == null) continue;
            double fluidHeight = getFluidHeight(block);
            if (fluidHeight >= 0) {
                var blockAbove = blockGetter.getBlock(posMut.blockX(), posMut.blockY() + 1,
                                                      posMut.blockZ(), Block.Getter.Condition.TYPE);
                fluidHeight = blockAbove != null && block.id() == blockAbove.id() ? 1 : (fluidHeight / 9.0);
                double surfaceY = posMut.blockY() + fluidHeight;
                if (surfaceY < position.y()) continue; // Not in fluid

                if (block.id() == Block.WATER.id() || BlockUtil.isWaterlogged(block)) {
                    inWater = true;
                    waterHeight = Math.max(waterHeight, surfaceY - position.y());
                } else if (block.id() == Block.LAVA.id()) {
                    inLava = true;
                    lavaHeight = Math.max(lavaHeight, surfaceY - position.y());
                }
            } else if (block.id() == Block.POWDER_SNOW.id()) {
                inPowderSnow = true;
            }
        }

        return new Result(inWater, inLava, inPowderSnow, waterHeight, lavaHeight);
    }

    /// Fluid height within a single block, decoded from the {@code level} property on an
    /// 8-scale, or -1 if the block is not a fluid. Waterlogged blocks are always full.
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
}
