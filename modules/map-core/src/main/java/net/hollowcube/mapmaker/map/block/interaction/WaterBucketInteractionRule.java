package net.hollowcube.mapmaker.map.block.interaction;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * Handles waterlogging and placing blocks of water using the bucket.
 */
public class WaterBucketInteractionRule implements BlockInteractionRule {

    @Override
    public @NotNull SneakState sneakState() {
        return SneakState.BOTH;
    }

    @Override
    public boolean handleInteraction(@NotNull Interaction interaction) {
        // Try to waterlog the block we clicked on
        var blockPosition = interaction.blockPosition();
        if (tryWaterlogBlock(interaction, blockPosition, true)) return true;

        // If the block is farmland, then we can make it wet
        var block = interaction.getBlock(blockPosition);
        if (block.id() == Block.FARMLAND.id()) {
            var newValue = "7".equals(block.getProperty("moisture")) ? "0" : "7";
            interaction.setBlock(blockPosition, block.withProperty("moisture", newValue));
            return true;
        }

        // Try to waterlog the neighbor block
        blockPosition = blockPosition.relative(interaction.blockFace());
        return tryWaterlogBlock(interaction, blockPosition, false);
    }

    private boolean tryWaterlogBlock(@NotNull Interaction interaction, @NotNull Point blockPosition, boolean strict) {
        var block = interaction.getBlock(blockPosition);

        // If the block is air, liquid, or adjacent and replaceable, just set it to a water block.
        if (block.isAir() || block.isLiquid() || (!strict && block.registry().isReplaceable())) {
            interaction.setBlock(blockPosition, Block.WATER);
            return true;
        }

        var waterloggedRaw = block.getProperty("waterlogged");
        if (waterloggedRaw == null) return false; // Cannot be waterlogged

        var waterlogged = Boolean.parseBoolean(waterloggedRaw);
        if (waterlogged) {
            // The vanilla behavior if you right click a waterlogged block with a bucket is to count the interaction.
            // If you have a water bucket in main hand and an empty bucket in off hand then click a waterlogged block
            // it will use the water bucket not the empty bucket.
            return true;
        }

        interaction.setBlock(blockPosition, block.withProperty("waterlogged", "true"));
        return true;
    }

}
