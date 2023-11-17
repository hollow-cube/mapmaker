package net.hollowcube.map.block.placement;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TwistingVinesPlacementRule extends BaseBlockPlacementRule {

    public TwistingVinesPlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        return block;
    }

    @Override
    public @NotNull Block blockUpdate(@NotNull UpdateState updateState) {
        Block block = updateState.currentBlock();
        Point position = updateState.blockPosition();
        int x = position.blockX();
        int y = position.blockY();
        int z = position.blockZ();

        var above = updateState.instance().getBlock(x, y + 1, z, Block.Getter.Condition.TYPE);
        if (above.id() == Block.TWISTING_VINES.id()) return Block.TWISTING_VINES_PLANT;

        return block;
    }

    @Override
    public int maxUpdateDistance() {
        return 1;
    }
}
