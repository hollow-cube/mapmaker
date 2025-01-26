package net.hollowcube.mapmaker.map.block.interaction;

import net.hollowcube.mapmaker.map.util.PlayerUtil;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

// This is needed to be an interaction rule instead of a placement rule due to the ability for it to be placed without
//  looking at a block
public class LilyPadInteractionRule implements BlockInteractionRule, BlockInteractionRule.AirInteractionRule {

    @Override
    public @NotNull SneakState sneakState() {
        return SneakState.BOTH;
    }

    @Override
    public boolean handleInteraction(@NotNull Interaction interaction) {
        return handleAirInteraction(interaction);
    }

    @Override
    public boolean handleAirInteraction(@NotNull Interaction interaction) {
        var player = interaction.player();
        var blockPosition = PlayerUtil.getTargetBlock(player, PlayerUtil.DEFAULT_PLACE_REACH);
        if (blockPosition == null) return false;

        var block = interaction.getBlock(blockPosition, Block.Getter.Condition.TYPE);

        if (block.isLiquid() && block.id() == Block.WATER.id()) {
            interaction.setBlock(blockPosition.add(0, 1, 0), Block.LILY_PAD);
            return true;
        }

        return false;
    }
}
