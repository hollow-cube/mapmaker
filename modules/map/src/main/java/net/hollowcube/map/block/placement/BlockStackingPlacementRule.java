package net.hollowcube.map.block.placement;

import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Also usable for sea pickles
 */
public class BlockStackingPlacementRule extends BaseBlockPlacementRule {
    private static final int MAX_AMOUNT = 4;

    public static final String CANDLE_PROPERTY = "candles";
    public static final String SEA_PICKLE_PROPERTY = "pickles";
    public static final String TURTLE_EGGS_PROPERTY = "eggs";

    private final String property;

    public BlockStackingPlacementRule(@NotNull Block block, @NotNull String property) {
        super(block);
        this.property = property;
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
        return block;
    }

    @Override
    public boolean isSelfReplaceable(@NotNull Replacement replacement) {
        var block = replacement.block();
        return Integer.parseInt(block.properties().get(property)) != MAX_AMOUNT;
    }
}
