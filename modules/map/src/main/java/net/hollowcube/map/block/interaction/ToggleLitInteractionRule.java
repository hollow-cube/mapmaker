package net.hollowcube.map.block.interaction;

import org.jetbrains.annotations.NotNull;

public class ToggleLitInteractionRule implements BlockInteractionRule {

    @Override
    public @NotNull SneakState sneakState() {
        return SneakState.SNEAKING;
    }

    @Override
    public boolean handleInteraction(@NotNull Interaction interaction) {
        var blockPosition = interaction.blockPosition();
        var block = interaction.getBlock(blockPosition);

        var newState = String.valueOf("false".equals(block.getProperty("lit")));
        interaction.setBlock(blockPosition, block.withProperty("lit", newState));
        return true;
    }

}
