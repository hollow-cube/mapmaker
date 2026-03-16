package net.hollowcube.mapmaker.map.block.interaction;

import java.util.List;

public class CopperGolemInteractionRule implements BlockInteractionRule {

    public static final CopperGolemInteractionRule INSTANCE = new CopperGolemInteractionRule();
    private static final List<String> STATES = List.of("standing", "sitting", "running", "star");

    private CopperGolemInteractionRule() {
    }

    @Override
    public boolean handleInteraction(Interaction interaction) {
        var blockPosition = interaction.blockPosition();
        var block = interaction.getBlock(blockPosition);

        var currState = block.getProperty("copper_golem_pose");
        var nextState = STATES.get((STATES.indexOf(currState) + 1) % STATES.size());

        interaction.setBlock(blockPosition, block.withProperty("copper_golem_pose", nextState));
        return true;
    }
}
