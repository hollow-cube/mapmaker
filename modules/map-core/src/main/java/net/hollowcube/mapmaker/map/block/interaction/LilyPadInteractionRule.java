package net.hollowcube.mapmaker.map.block.interaction;

import net.hollowcube.common.util.PlayerUtil;
import net.minestom.server.instance.block.Block;

// This is needed to be an interaction rule instead of a placement rule due to the ability for it to be placed without
//  looking at a block
public class LilyPadInteractionRule implements BlockInteractionRule, BlockInteractionRule.AirInteractionRule {

    @Override
    public SneakState sneakState() {
        return SneakState.BOTH;
    }

    @Override
    public boolean handleInteraction(Interaction interaction) {
        return handleAirInteraction(interaction);
    }

    @Override
    public boolean handleAirInteraction(Interaction interaction) {
        var player = interaction.player();
        var blockPosition = PlayerUtil.getTargetBlock(player, PlayerUtil.DEFAULT_PLACEMENT_DISTANCE, true);
        if (blockPosition == null) return false;

        var block = interaction.getBlock(blockPosition, Block.Getter.Condition.TYPE);

        if (block.isLiquid() && block.id() == Block.WATER.id()) {
            interaction.setBlock(blockPosition.add(0, 1, 0), Block.LILY_PAD);
            return true;
        }

        return false;
    }
}
