package net.hollowcube.mapmaker.map.block.placement;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

public final class PaleHangingMossPlacementRule extends BaseBlockPlacementRule {

    public PaleHangingMossPlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @NotNull Block blockPlace(@NotNull PlacementState placementState) {
        return computeState(placementState.instance(), placementState.placePosition(), this.block);
    }

    @Override
    public @NotNull Block blockUpdate(@NotNull UpdateState updateState) {
        return computeState(updateState.instance(), updateState.blockPosition(), updateState.currentBlock());
    }

    @Override
    public int maxUpdateDistance() {
        return 1;
    }

    private @NotNull Block computeState(@NotNull Block.Getter instance, @NotNull Point blockPosition, @NotNull Block block) {
        int x = blockPosition.blockX();
        int y = blockPosition.blockY();
        int z = blockPosition.blockZ();

        var below = instance.getBlock(x, y - 1, z, Block.Getter.Condition.TYPE);
        if (below.id() == this.block.id()) return block.withProperty("tip", "false");
        return block;
    }
}
