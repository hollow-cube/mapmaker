package net.hollowcube.mapmaker.map.block.placement;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.jetbrains.annotations.Nullable;

public class AlwaysReplacePlacementRule extends BlockPlacementRule {

    public AlwaysReplacePlacementRule(Block block) {
        super(block);
    }

    @Override
    public @Nullable Block blockPlace(PlacementState placementState) {
        return block;
    }

    @Override
    public boolean isSelfReplaceable(Replacement replacement) {
        return true;
    }
}
