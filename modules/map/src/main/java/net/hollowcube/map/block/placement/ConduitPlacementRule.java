package net.hollowcube.map.block.placement;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// This exists exclusively so that the BlockHandler is applied on place
public class ConduitPlacementRule extends BlockPlacementRule {

    public ConduitPlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @Nullable Block blockPlace(@NotNull BlockPlacementRule.PlacementState placement) {
        return block;
    }

}
