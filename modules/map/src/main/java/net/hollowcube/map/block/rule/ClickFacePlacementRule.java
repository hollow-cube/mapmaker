package net.hollowcube.map.block.rule;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ClickFacePlacementRule extends BlockPlacementRule {
    public static final String PROP_FACING = "facing";

    public ClickFacePlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        return block.withProperty(PROP_FACING, placementState.blockFace().name().toLowerCase());
    }
}
