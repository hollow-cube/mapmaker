package net.hollowcube.mapmaker.map.block.interaction;

import net.hollowcube.mapmaker.map.util.PlayerUtil;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class EmptyBucketInteractionRule implements BlockInteractionRule, BlockInteractionRule.AirInteractionRule {

    @Override
    public @NotNull SneakState sneakState() {
        return SneakState.BOTH;
    }

    @Override
    public boolean handleInteraction(@NotNull Interaction interaction) {
        return false;
    }

    @Override
    public boolean handleAirInteraction(@NotNull Interaction interaction) {
        var player = interaction.player();
        var blockPosition = PlayerUtil.getTargetBlock(player, PlayerUtil.DEFAULT_PLACE_REACH);
        if (blockPosition == null) return false;

        var block = interaction.getBlock(blockPosition, Block.Getter.Condition.TYPE);

        // If the clicked a liquid directly, remove it.
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
