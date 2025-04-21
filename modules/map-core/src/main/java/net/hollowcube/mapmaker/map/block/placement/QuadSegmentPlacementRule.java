package net.hollowcube.mapmaker.map.block.placement;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

public class QuadSegmentPlacementRule extends FacingHorizontalPlacementRule {
    private static final int MAX_SEGMENTS = 4;

    private final String segmentProperty;

    public QuadSegmentPlacementRule(@NotNull Block block, @NotNull String segmentProperty) {
        super(block, true);
        this.segmentProperty = segmentProperty;
    }

    @Override
    public @UnknownNullability Block blockPlace(@NotNull PlacementState placementState) {
        var existingBlock = placementState.instance().getBlock(placementState.placePosition());
        if (existingBlock.id() == block.id()) {
            // There is already a flower, and we are replacing it, increment the candle count
            var amount = Integer.parseInt(existingBlock.properties().get(segmentProperty));
            if (amount == MAX_SEGMENTS) return null;
            return existingBlock.withProperty(segmentProperty, String.valueOf(amount + 1));
        }

        return super.blockPlace(placementState);
    }

    @Override
    public boolean isSelfReplaceable(@NotNull BlockPlacementRule.Replacement replacement) {
        if (replacement.material().isBlock() && replacement.material().block().id() == this.block.id())
            return Integer.parseInt(replacement.block().getProperty(segmentProperty)) <= MAX_SEGMENTS;
        return super.isSelfReplaceable(replacement);
    }
}
