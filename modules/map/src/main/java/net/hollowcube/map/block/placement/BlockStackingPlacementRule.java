package net.hollowcube.map.block.placement;

import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Also usable for sea pickles
 */
public class BlockStackingPlacementRule extends BaseBlockPlacementRule {
    private static final int MAX_AMOUNT = 4;

    //todo sea pickles should be waterlogged by default if placed in water, otherwise not. I think this is the case for all waterlogg-able blocks.

    public static final String CANDLE_PROPERTY = "candles";
    public static final String SEA_PICKLE_PROPERTY = "pickles";
    public static final String TURTLE_EGGS_PROPERTY = "eggs";

    private final String property;
    private final boolean canBeWaterlogged;

    public BlockStackingPlacementRule(@NotNull Block block, @NotNull String property) {
        super(block);
        this.property = property;
        this.canBeWaterlogged = block.getProperty("waterlogged") != null;
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        var existingBlock = placementState.instance().getBlock(placementState.placePosition());
        if (existingBlock.compare(block)) {
            // There is already a candle/sea pickle, and we are replacing it, increment the candle count
            var amount = Integer.parseInt(existingBlock.properties().get(property));
            if (amount == MAX_AMOUNT) return null;
            return existingBlock.withProperty(property, String.valueOf(amount + 1));
        }

        if (canBeWaterlogged) {
            var waterlogged = String.valueOf(existingBlock.id() == Block.WATER.id());
            return block.withProperty("waterlogged", waterlogged);
        }

        return block;
    }

    @Override
    public boolean isSelfReplaceable(@NotNull Replacement replacement) {
        if (replacement.material().isBlock() && replacement.material().block().id() == this.block.id())
            return Integer.parseInt(replacement.block().properties().get(property)) != MAX_AMOUNT;
        return false;
    }
}
