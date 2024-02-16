package net.hollowcube.map2.block.interaction;

import net.hollowcube.map2.block.interaction
        .BlockInteractionRule;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractToggleInteractionRule implements BlockInteractionRule {
    @Override
    public boolean handleInteraction(@NotNull Interaction interaction) {
        var blockPosition = interaction.blockPosition();
        var block = interaction.getBlock(blockPosition);

        var newState = !"true".equals(block.getProperty("powered"));
        interaction.setBlock(blockPosition, block.withProperty("powered", String.valueOf(newState)));
        playSound(interaction, block, newState);

        return true;
    }

    @Override
    public @NotNull SneakState sneakState() {
        return SneakState.NOT_SNEAKING_OR_EMPTY_HAND;
    }

    protected abstract void playSound(@NotNull Interaction interaction, @NotNull Block block, boolean newState);

}
