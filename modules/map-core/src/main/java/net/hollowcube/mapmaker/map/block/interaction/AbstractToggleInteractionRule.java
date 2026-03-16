package net.hollowcube.mapmaker.map.block.interaction;

import net.minestom.server.instance.block.Block;

public abstract class AbstractToggleInteractionRule implements BlockInteractionRule {
    @Override
    public boolean handleInteraction(Interaction interaction) {
        var blockPosition = interaction.blockPosition();
        var block = interaction.getBlock(blockPosition);

        var newState = !"true".equals(block.getProperty("powered"));
        interaction.setBlock(blockPosition, block.withProperty("powered", String.valueOf(newState)));
        playSound(interaction, block, newState);

        return true;
    }

    @Override
    public SneakState sneakState() {
        return SneakState.NOT_SNEAKING_OR_EMPTY_HAND;
    }

    protected abstract void playSound(Interaction interaction, Block block, boolean newState);

}
