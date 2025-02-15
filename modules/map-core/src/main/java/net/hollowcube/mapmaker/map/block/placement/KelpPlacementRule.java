package net.hollowcube.mapmaker.map.block.placement;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class KelpPlacementRule extends BaseBlockPlacementRule {

    public KelpPlacementRule(@NotNull Block block) {
        super(block);
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

        var posAbove = placementState.placePosition().add(0, 1, 0);
        var blockAbove = placementState.instance().getBlock(posAbove, Block.Getter.Condition.TYPE);

        var isKelpAbove = blockAbove.id() == Block.KELP.id() || blockAbove.id() == Block.KELP_PLANT.id();
        var isKelpBelow = blockBelow.id() == Block.KELP.id() || blockBelow.id() == Block.KELP_PLANT.id();

        if (isKelpAbove && isKelpBelow) {
            return Block.KELP_PLANT;
        } else if (isKelpAbove) {
            return Block.KELP_PLANT;
        }

        return this.block;
    }

    @Override
    public int maxUpdateDistance() {
        return 1;
    }
}
