package net.hollowcube.mapmaker.map.block.interaction;

public class ToggleLitInteractionRule implements BlockInteractionRule {

    @Override
    public SneakState sneakState() {
        return SneakState.SNEAKING;
    }

    @Override
    public boolean handleInteraction(Interaction interaction) {
        var item = interaction.item();
        if (item.material().isBlock()) return false;
        var blockPosition = interaction.blockPosition();
        var block = interaction.getBlock(blockPosition);

        var newState = String.valueOf("false".equals(block.getProperty("lit")));
        interaction.setBlock(blockPosition, block.withProperty("lit", newState));
        return true;
    }

}
