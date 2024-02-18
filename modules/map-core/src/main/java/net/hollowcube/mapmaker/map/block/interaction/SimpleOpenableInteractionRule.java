package net.hollowcube.mapmaker.map.block.interaction;

import org.jetbrains.annotations.NotNull;

public class SimpleOpenableInteractionRule implements BlockInteractionRule {
    public static final SimpleOpenableInteractionRule INSTANCE = new SimpleOpenableInteractionRule();

    private SimpleOpenableInteractionRule() {
    }

    @Override
    public boolean handleInteraction(@NotNull Interaction interaction) {
        var blockPosition = interaction.blockPosition();
        var block = interaction.getBlock(blockPosition);

        var open = Boolean.parseBoolean(block.getProperty("open"));
        block = block.withProperty("open", String.valueOf(!open));

        interaction.setBlock(blockPosition, block);
        return true;
    }

}
