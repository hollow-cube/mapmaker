package net.hollowcube.mapmaker.map.block.placement;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class KelpPlacementRule extends FacingHorizontalPlacementRule {

    public KelpPlacementRule(@NotNull Block block) {
        super(block, true);
    }

    @Override
    public @NotNull Block blockUpdate(@NotNull UpdateState updateState) {
        var currentBlock = updateState.currentBlock();
        if (updateState.fromFace() != BlockFace.TOP) return currentBlock;

        var posAbove = updateState.blockPosition().add(0, 1, 0);
        var blockAbove = updateState.instance().getBlock(posAbove, Block.Getter.Condition.TYPE);
        if (blockAbove.id() == Block.KELP.id() || blockAbove.id() == Block.KELP_PLANT.id()) {
            return Block.KELP_PLANT;
        }

        return currentBlock;
    }

    @Override
    public @NotNull Block blockPlace(@NotNull PlacementState placementState) {
        var posBelow = placementState.placePosition().add(0, -1, 0);
        var blockBelow = placementState.instance().getBlock(posBelow, Block.Getter.Condition.TYPE);
        if (blockBelow.id() == Block.KELP.id() || blockBelow.id() == Block.KELP_PLANT.id())
            // If below has facing, use that.
            return this.block;

        var posAbove = placementState.placePosition().add(0, 1, 0);
        var blockAbove = placementState.instance().getBlock(posAbove, Block.Getter.Condition.TYPE);
        if (blockAbove.id() == Block.KELP.id() || blockAbove.id() == Block.KELP_PLANT.id())
            // If above has facing, use that.
            return this.block;

        // Otherwise, use the inverted player facing handled by the superclass.
        return super.blockPlace(placementState);
    }

    @Override
    public int maxUpdateDistance() {
        return 1;
    }
}
