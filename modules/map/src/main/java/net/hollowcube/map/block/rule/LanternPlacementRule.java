package net.hollowcube.map.block.rule;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LanternPlacementRule extends BlockPlacementRule {
    private static final String PROP_HANGING = "hanging";

    public LanternPlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @NotNull Block blockUpdate(@NotNull UpdateState updateState) {
        return updateState.currentBlock();
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        var instance = placementState.instance();

        // Try to place on the block below
        var blockBelow = instance.getBlock(placementState.placePosition().add(0, -1, 0), Block.Getter.Condition.TYPE);
        if (blockBelow.isSolid()) return block.withProperty(PROP_HANGING, "false");

        // Try to place on the block above
        var blockAbove = instance.getBlock(placementState.placePosition().add(0, 1, 0), Block.Getter.Condition.TYPE);
        if (blockAbove.isSolid()) return block.withProperty(PROP_HANGING, "true");

        return block;
    }
}
