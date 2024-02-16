package net.hollowcube.map2.block.interaction;

import net.hollowcube.map2.block.BlockTags;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class ShovelInteractionRule implements net.hollowcube.map2.block.interaction.BlockInteractionRule {

    @Override
    public @NotNull SneakState sneakState() {
        return SneakState.BOTH;
    }

    @Override
    public boolean handleInteraction(@NotNull Interaction interaction) {
        var blockPosition = interaction.blockPosition();
        var block = interaction.getBlock(blockPosition, Block.Getter.Condition.TYPE);

        if (BlockTags.DIRT_PATH_CONVERTABLE.contains(block.namespace())) {
            interaction.setBlock(blockPosition, Block.DIRT_PATH);
            return true;
        }

        return false;
    }
}
