package net.hollowcube.mapmaker.map.block.interaction;

import net.hollowcube.mapmaker.map.block.BlockTags;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class HoeInteractionRule implements BlockInteractionRule {
    public static final HoeInteractionRule INSTANCE = new HoeInteractionRule();

    private HoeInteractionRule() {
    }

    @Override
    public @NotNull SneakState sneakState() {
        return SneakState.BOTH;
    }

    @Override
    public boolean handleInteraction(@NotNull Interaction interaction) {
        var blockPosition = interaction.blockPosition();
        var block = interaction.getBlock(blockPosition, Block.Getter.Condition.TYPE);

        if (BlockTags.FARMLAND_CONVERTABLE.contains(block.namespace())) {
            interaction.setBlock(blockPosition, Block.FARMLAND);
            return true;
        } else if (BlockTags.DIRT_CONVERTABLE.contains(block.namespace())) {
            interaction.setBlock(blockPosition, Block.DIRT);
            return true;
        }

        return false;
    }

}
