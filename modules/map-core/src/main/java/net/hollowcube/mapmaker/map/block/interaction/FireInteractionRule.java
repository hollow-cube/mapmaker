package net.hollowcube.mapmaker.map.block.interaction;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;

public class FireInteractionRule implements BlockInteractionRule {

    @Override
    public SneakState sneakState() {
        return SneakState.BOTH;
    }

    @Override
    public boolean handleInteraction(Interaction interaction) {
        var blockPosition = interaction.blockPosition();
        if (tryPlaceFire(interaction, blockPosition)) return true;

        blockPosition = blockPosition.relative(interaction.blockFace());
        return tryPlaceFire(interaction, blockPosition);
    }

    private boolean tryPlaceFire(Interaction interaction, Point blockPosition) {
        var block = interaction.getBlock(blockPosition);
        if (!block.isAir()) return false;

        var belowBlock = interaction.getBlock(blockPosition.add(0, -1, 0), Block.Getter.Condition.TYPE);
        var fireBlock = belowBlock.id() == Block.SOUL_SAND.id() || belowBlock.id() == Block.SOUL_SOIL.id()
                ? Block.SOUL_FIRE : Block.FIRE;
        interaction.placeBlock(blockPosition, fireBlock);
        return true;
    }

}
