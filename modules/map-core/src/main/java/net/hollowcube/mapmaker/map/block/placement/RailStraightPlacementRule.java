package net.hollowcube.mapmaker.map.block.placement;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RailStraightPlacementRule extends BaseBlockPlacementRule {
    public RailStraightPlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @Nullable Block blockPlace(@NotNull BlockPlacementRule.PlacementState placement) {
        return null;
    }
}
