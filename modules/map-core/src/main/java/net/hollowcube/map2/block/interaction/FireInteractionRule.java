package net.hollowcube.map2.block.interaction;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class FireInteractionRule implements net.hollowcube.map2.block.interaction.BlockInteractionRule {

    @Override
    public @NotNull SneakState sneakState() {
        return SneakState.BOTH;
    }

    @Override
    public boolean handleInteraction(@NotNull Interaction interaction) {
        var blockPosition = interaction.blockPosition();
        if (tryPlaceFire(interaction, blockPosition)) return true;

        blockPosition = blockPosition.relative(interaction.blockFace());
        return tryPlaceFire(interaction, blockPosition);
    }

    private boolean tryPlaceFire(@NotNull Interaction interaction, @NotNull Point blockPosition) {
        var block = interaction.getBlock(blockPosition);
        if (!block.isAir()) return false;

        var belowBlock = interaction.getBlock(blockPosition.add(0, -1, 0), Block.Getter.Condition.TYPE);
        var fireBlock = belowBlock.id() == Block.SOUL_SAND.id() || belowBlock.id() == Block.SOUL_SOIL.id()
                ? Block.SOUL_FIRE : Block.FIRE;
        interaction.setBlock(blockPosition, fireBlock);
        return true;
    }

}
