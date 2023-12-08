package net.hollowcube.map.block.interaction;

import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

public class EmptyBucketInteractionRule implements BlockInteractionRule {

    @Override
    public boolean handleInteraction(@NotNull Interaction interaction) {
        var blockPosition = interaction.blockPosition();
        var block = interaction.getBlock(blockPosition);
        if (block.isLiquid()) {
            interaction.setBlock(blockPosition, Block.AIR);
            return true;
        }

        // If the block is waterlog-able and waterlogged, remove the waterlog.
        if ("true".equals(block.getProperty("waterlogged"))) {
            interaction.setBlock(blockPosition, block.withProperty("waterlogged", "false"));
            return true;
        }

        return false;
    }

}
