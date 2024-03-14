package net.hollowcube.mapmaker.map.block.placement;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

public class PinkPetalPlacementRule extends FacingHorizontalPlacementRule {
    private static final int MAX_FLOWERS = 4;

    public PinkPetalPlacementRule(@NotNull Block block) {
        super(block, true);
    }

    @Override
    public @UnknownNullability Block blockPlace(@NotNull PlacementState placementState) {
        var existingBlock = placementState.instance().getBlock(placementState.placePosition());
        if (existingBlock.id() == block.id()) {
            // There is already a flower, and we are replacing it, increment the candle count
            var amount = Integer.parseInt(existingBlock.properties().get("flower_amount"));
            if (amount == MAX_FLOWERS) return null;
            return existingBlock.withProperty("flower_amount", String.valueOf(amount + 1));
        }

        return super.blockPlace(placementState);
    }

    @Override
    public boolean isSelfReplaceable(@NotNull BlockPlacementRule.Replacement replacement) {
        if (replacement.material().isBlock() && replacement.material().block().id() == this.block.id())
            return Integer.parseInt(replacement.block().getProperty("flower_amount")) <= MAX_FLOWERS;
        return super.isSelfReplaceable(replacement);
    }
}
