package net.hollowcube.mapmaker.map.block.interaction;

public class DaylightDetectorInteractionRule implements BlockInteractionRule {
    @Override
    public boolean handleInteraction(Interaction interaction) {
        var block = interaction.getBlock(interaction.blockPosition());

        var prop = Boolean.parseBoolean(block.getProperty("inverted"));
        block = block.withProperty("inverted", String.valueOf(!prop));

        interaction.setBlock(interaction.blockPosition(), block);
        return true;
    }
}
