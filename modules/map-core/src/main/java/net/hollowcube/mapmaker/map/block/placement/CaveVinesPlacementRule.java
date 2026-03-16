package net.hollowcube.mapmaker.map.block.placement;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.Nullable;

public final class CaveVinesPlacementRule extends BaseBlockPlacementRule {

    public CaveVinesPlacementRule(Block block) {
        super(block);
    }

    @Override
    public @Nullable Block blockPlace(PlacementState placementState) {
        if (placementState.blockFace() != BlockFace.BOTTOM) return null;
        return Block.CAVE_VINES;
    }

    @Override
    public Block blockUpdate(UpdateState updateState) {
        Block block = updateState.currentBlock();
        Point position = updateState.blockPosition();
        int x = position.blockX();
        int y = position.blockY();
        int z = position.blockZ();

        Block below = updateState.instance().getBlock(x, y - 1, z);
        if (below.compare(Block.CAVE_VINES)) return Block.CAVE_VINES_PLANT;

        return block;
    }

    @Override
    public int maxUpdateDistance() {
        return 1;
    }
}
