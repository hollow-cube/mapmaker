package net.hollowcube.mapmaker.map.block.placement;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

public class WaterloggedPlacementRule extends BaseBlockPlacementRule {

    public WaterloggedPlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @UnknownNullability Block blockPlace(@NotNull BlockPlacementRule.PlacementState placement) {
        // Editor note: All logic should be in the waterlogged function. Some rules call that function instead of this one.
        return block.withProperty("waterlogged", waterlogged(placement));
    }

    protected String waterlogged(@NotNull PlacementState placement) {
        return waterlogged(placement.instance().getBlock(placement.placePosition(), Block.Getter.Condition.TYPE));
    }

    protected String waterlogged(@NotNull Block existing) {
        return String.valueOf(existing.id() == Block.WATER.id());
    }

}
