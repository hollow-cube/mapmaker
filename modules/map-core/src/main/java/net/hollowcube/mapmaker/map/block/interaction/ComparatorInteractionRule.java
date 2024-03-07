package net.hollowcube.mapmaker.map.block.interaction;

import org.jetbrains.annotations.NotNull;

public class ComparatorInteractionRule implements BlockInteractionRule {

    @Override
    public boolean handleInteraction(@NotNull Interaction interaction) {
        var blockPosition = interaction.blockPosition();
        var block = interaction.getBlock(blockPosition);
        var newMode = "compare".equals(block.getProperty("mode")) ? "subtract" : "compare";

        var newBlock = block.withProperty("mode", newMode);
        interaction.setBlock(blockPosition, newBlock);
        return true;
    }

    @Override
    public @NotNull SneakState sneakState() {
        return SneakState.NOT_SNEAKING_OR_EMPTY_HAND;
    }

}
