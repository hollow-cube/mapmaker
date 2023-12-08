package net.hollowcube.map.block.interaction;

import org.jetbrains.annotations.NotNull;

public class CakeInteractionRule implements BlockInteractionRule {

    @Override
    public boolean handleInteraction(@NotNull Interaction interaction) {
        var blockPosition = interaction.blockPosition();
        var block = interaction.getBlock(blockPosition);

        var bites = Integer.parseInt(block.getProperty("bites"));
        block = block.withProperty("bites", String.valueOf((bites + 1) % 7));
        interaction.setBlock(blockPosition, block);

        return true;
    }

}
