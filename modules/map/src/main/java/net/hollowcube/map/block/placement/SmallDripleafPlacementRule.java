package net.hollowcube.map.block.placement;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
public class SmallDripleafPlacementRule extends FacingHorizontalPlacementRule {
    private static final String PROP_FACING = "facing";
    private static final String PROP_HALF = "half"; // lower/upper

    public SmallDripleafPlacementRule(@NotNull Block block) {
        super(block.withProperty(PROP_HALF, "upper"), true);
    }

    @Override
    public @NotNull Block blockUpdate(@NotNull UpdateState updateState) {
        var currentBlock = updateState.currentBlock();
        if (updateState.fromFace() != BlockFace.TOP) return currentBlock;

        var posAbove = updateState.blockPosition().add(0, 1, 0);
        var blockAbove = updateState.instance().getBlock(posAbove, Block.Getter.Condition.TYPE);
        if (blockAbove.id() == this.block.id()) {
            return currentBlock.withProperty(PROP_HALF, "lower");
        }

        return currentBlock;
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        var posBelow = placementState.placePosition().add(0, -1, 0);
        var blockBelow = placementState.instance().getBlock(posBelow, Block.Getter.Condition.TYPE);
        if (blockBelow.id() == this.block.id())
            // If below has facing, use that.
            return this.block.withProperty(PROP_FACING, blockBelow.getProperty(PROP_FACING));

        var posAbove = placementState.placePosition().add(0, 1, 0);
        var blockAbove = placementState.instance().getBlock(posAbove, Block.Getter.Condition.TYPE);
        if (blockAbove.id() == this.block.id())
            // If above has facing, use that.
            return this.block.withProperty(PROP_FACING, blockAbove.getProperty(PROP_FACING));

        // Otherwise, use the inverted player facing handled by the superclass.
        return super.blockPlace(placementState);
    }

    @Override
    public int maxUpdateDistance() {
        return 1;
    }

}
