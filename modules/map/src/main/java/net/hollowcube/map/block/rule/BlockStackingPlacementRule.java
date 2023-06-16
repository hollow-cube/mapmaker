package net.hollowcube.map.block.rule;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Also usable for sea pickles */
public class BlockStackingPlacementRule extends BlockPlacementRule {
    private static final int MAX_AMOUNT = 4;

    public static final String CANDLE_PROPERTY = "candles";
    public static final String SEA_PICKLE_PROPERTY = "pickles";

    private final String property;

    public BlockStackingPlacementRule(@NotNull Block block, @NotNull String property) {
        super(block);
        this.property = property;
    }

    @Override
    public @NotNull Block blockUpdate(@NotNull UpdateState updateState) {
        return updateState.currentBlock();
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
    public boolean isSelfReplaceable(@NotNull Block block, @NotNull BlockFace blockFace, @NotNull Point cursorPosition) {
        return Integer.parseInt(block.properties().get(property)) != MAX_AMOUNT;
    }
}
