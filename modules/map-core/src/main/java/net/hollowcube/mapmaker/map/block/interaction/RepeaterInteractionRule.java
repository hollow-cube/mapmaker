package net.hollowcube.mapmaker.map.block.interaction;

public class RepeaterInteractionRule implements BlockInteractionRule {
    @Override
    public boolean handleInteraction(Interaction interaction) {
        var blockPosition = interaction.blockPosition();
        var block = interaction.getBlock(blockPosition);
        var delay = Integer.parseInt(block.getProperty("delay"));

        var newBlock = block.withProperty("delay", String.valueOf((delay % 4) + 1));
        interaction.setBlock(blockPosition, newBlock);
        return true;
    }

    @Override
    public SneakState sneakState() {
        return SneakState.NOT_SNEAKING_OR_EMPTY_HAND;
    }
}
