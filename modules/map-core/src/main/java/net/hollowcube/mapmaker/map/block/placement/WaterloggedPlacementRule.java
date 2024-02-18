package net.hollowcube.mapmaker.map.block.placement;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WaterloggedPlacementRule extends BlockPlacementRule {

    public WaterloggedPlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @Nullable Block blockPlace(@NotNull BlockPlacementRule.PlacementState placement) {
        var existing = placement.instance().getBlock(placement.placePosition(), Block.Getter.Condition.TYPE);

        return block.withProperty("waterlogged", String.valueOf(existing.id() == Block.WATER.id()));
    }

}
