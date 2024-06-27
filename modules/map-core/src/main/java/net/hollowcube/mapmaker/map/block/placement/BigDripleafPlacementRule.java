package net.hollowcube.mapmaker.map.block.placement;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public class BigDripleafPlacementRule extends FacingHorizontalPlacementRule {
    private static final String PROP_FACING = "facing";

    public BigDripleafPlacementRule(@NotNull Block block) {
        super(block, true);
    }

    @Override
    public @NotNull Block blockUpdate(@NotNull UpdateState updateState) {
        var currentBlock = updateState.currentBlock();
        if (updateState.fromFace() != BlockFace.TOP) return currentBlock;

        var posAbove = updateState.blockPosition().add(0, 1, 0);
        var blockAbove = updateState.instance().getBlock(posAbove, Block.Getter.Condition.TYPE);
        if (blockAbove.id() == Block.BIG_DRIPLEAF.id() || blockAbove.id() == Block.BIG_DRIPLEAF_STEM.id()) {
            var worldBlock = updateState.instance().getBlock(updateState.blockPosition(), Block.Getter.Condition.TYPE);
            return Block.BIG_DRIPLEAF_STEM.withProperties(Map.of(
                    PROP_FACING, currentBlock.getProperty(PROP_FACING),
                    "waterlogged", waterlogged(worldBlock)
            ));
        }

        return currentBlock;
    }

    @Override
    public @NotNull Block blockPlace(@NotNull PlacementState placementState) {
        var posBelow = placementState.placePosition().add(0, -1, 0);
        var blockBelow = placementState.instance().getBlock(posBelow, Block.Getter.Condition.TYPE);
        if (blockBelow.id() == Block.BIG_DRIPLEAF.id() || blockBelow.id() == Block.BIG_DRIPLEAF_STEM.id())
            // If below has facing, use that.
            return this.block.withProperty(PROP_FACING, blockBelow.getProperty(PROP_FACING));

        var posAbove = placementState.placePosition().add(0, 1, 0);
        var blockAbove = placementState.instance().getBlock(posAbove, Block.Getter.Condition.TYPE);
        if (blockAbove.id() == Block.BIG_DRIPLEAF.id() || blockAbove.id() == Block.BIG_DRIPLEAF_STEM.id())
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
