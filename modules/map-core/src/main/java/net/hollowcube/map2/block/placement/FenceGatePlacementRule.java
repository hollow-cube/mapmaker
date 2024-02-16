package net.hollowcube.map2.block.placement;

import net.hollowcube.map2.block.BlockTags;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class FenceGatePlacementRule extends FacingHorizontalPlacementRule {
    private static final List<BlockFace> HORIZONTAL_FACES = List.of(
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST
    );

    private static final String PROP_IN_WALL = "in_wall";

    public FenceGatePlacementRule(@NotNull Block block) {
        super(block, false);
    }

    @Override
    public @NotNull Block blockUpdate(@NotNull UpdateState updateState) {
        return genericUpdateState(updateState.instance(), updateState.currentBlock(), updateState.blockPosition());
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        return genericUpdateState(placementState.instance(), super.blockPlace(placementState), placementState.placePosition());
    }

    private @NotNull Block genericUpdateState(@NotNull Block.Getter instance, @NotNull Block block, @NotNull Point blockPosition) {
        var facing = BlockFace.valueOf(block.getProperty("facing").toUpperCase());
        facing = HORIZONTAL_FACES.get((HORIZONTAL_FACES.indexOf(facing) + 1) % 4); // Get clockwise direction of gate
        var inWall = isWall(instance, blockPosition, facing) || isWall(instance, blockPosition, facing.getOppositeFace());
        return block.withProperty(PROP_IN_WALL, String.valueOf(inWall));
    }

    private boolean isWall(@NotNull Block.Getter instance, @NotNull Point pos, @NotNull BlockFace blockFace) {
        var block = instance.getBlock(pos.relative(blockFace), Block.Getter.Condition.TYPE);
        return BlockTags.WALLS.contains(block.namespace());
    }

    @Override
    public int maxUpdateDistance() {
        return 1;
    }

    public static boolean isConnectableGate(@NotNull Block block, @NotNull BlockFace fenceFace) {
        if (!BlockTags.FENCE_GATES.contains(block.namespace()))
            return false;

        var facing = BlockFace.valueOf(block.getProperty("facing").toUpperCase());
        facing = HORIZONTAL_FACES.get((HORIZONTAL_FACES.indexOf(facing) + 1) % 4); // Get clockwise direction of gate
        return facing.isSimilar(fenceFace);
    }
}
