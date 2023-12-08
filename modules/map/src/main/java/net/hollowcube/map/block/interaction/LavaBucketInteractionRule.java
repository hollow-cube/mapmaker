package net.hollowcube.map.block.interaction;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

public class LavaBucketInteractionRule implements BlockInteractionRule {

    @Override
    public @NotNull SneakState sneakState() {
        return SneakState.BOTH;
    }

    @Override
    public boolean handleInteraction(@NotNull Interaction interaction) {
        // Try to place on the block we clicked. This is required for replacement
        // (eg left + right click at the same time to replace the block)
        var blockPosition = interaction.blockPosition();
        if (tryPlaceLava(interaction, blockPosition, true)) return true;

        // Try to place on the neighbor block
        blockPosition = blockPosition.relative(interaction.blockFace());
        return tryPlaceLava(interaction, blockPosition, false);
    }

    private boolean tryPlaceLava(@NotNull Interaction interaction, @NotNull Point blockPosition, boolean strict) {
        var block = interaction.getBlock(blockPosition);
        if (!block.isAir() && !block.isLiquid() && strict) return false;

        // In non-strict mode (when placing on a neighbor) we can replace replaceable blocks.
        if (!strict && !block.registry().isReplaceable()) return false;

        interaction.setBlock(blockPosition, Block.LAVA);
        return true;
    }


}
