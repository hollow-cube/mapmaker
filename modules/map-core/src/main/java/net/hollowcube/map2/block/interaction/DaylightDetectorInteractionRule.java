package net.hollowcube.map2.block.interaction;

import org.jetbrains.annotations.NotNull;

public class DaylightDetectorInteractionRule implements net.hollowcube.map2.block.interaction.BlockInteractionRule {
    @Override
    public boolean handleInteraction(@NotNull Interaction interaction) {
        var block = interaction.getBlock(interaction.blockPosition());

        var prop = Boolean.parseBoolean(block.getProperty("inverted"));
        block = block.withProperty("inverted", String.valueOf(!prop));

        interaction.setBlock(interaction.blockPosition(), block);
        return true;
    }
}
