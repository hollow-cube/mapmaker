package net.hollowcube.map.block.rule;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AxisPlacementRule extends BlockPlacementRule {
    private static final String PROP_AXIS = "axis";

    public AxisPlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @NotNull Block blockUpdate(@NotNull UpdateState updateState) {
        return updateState.currentBlock();
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        return block.withProperty(PROP_AXIS, switch (placementState.blockFace()) {
            case WEST, EAST -> "x";
            case SOUTH, NORTH -> "z";
            case TOP, BOTTOM -> "y";
        });
    }
}
