package net.hollowcube.map.block.placement;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
public final class TwistingVinesPlacementRule extends BaseBlockPlacementRule {

    public TwistingVinesPlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placement) {
        var blockPosition = placement.placePosition();
        var above = placement.instance().getBlock(blockPosition.add(0, 1, 0), Block.Getter.Condition.TYPE);
        if (above.id() == Block.TWISTING_VINES.id()) return Block.TWISTING_VINES_PLANT;
        return block;
    }

    @Override
    public @NotNull Block blockUpdate(@NotNull UpdateState updateState) {
        Block block = updateState.currentBlock();
        Point blockPosition = updateState.blockPosition();

        var above = updateState.instance().getBlock(blockPosition.add(0, 1, 0), Block.Getter.Condition.TYPE);
        if (above.id() == Block.TWISTING_VINES.id()) return Block.TWISTING_VINES_PLANT;

        return block;
    }

    @Override
    public int maxUpdateDistance() {
        return 1;
    }
}
