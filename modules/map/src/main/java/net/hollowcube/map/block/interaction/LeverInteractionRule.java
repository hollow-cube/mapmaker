package net.hollowcube.map.block.interaction;

import org.jetbrains.annotations.NotNull;

public class LeverInteractionRule implements BlockInteractionRule {
    @Override
    public boolean handleInteraction(@NotNull Interaction interaction) {
        var blockPosition = interaction.blockPosition();
        var block = interaction.getBlock(blockPosition);

        var newState = String.valueOf(!"true".equals(block.getProperty("powered")));
        interaction.setBlock(blockPosition, block.withProperty("powered", newState));
        return true;
    }

    @Override
    public @NotNull SneakState sneakState() {
        return SneakState.NOT_SNEAKING_OR_EMPTY_HAND;
    }
}
