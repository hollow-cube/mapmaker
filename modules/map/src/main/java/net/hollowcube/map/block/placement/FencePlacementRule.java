package net.hollowcube.map.block.placement;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class FencePlacementRule extends BaseBlockPlacementRule {
    private static final List<BlockFace> HORIZONTAL_FACES = List.of(
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST
    );

    public FencePlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @NotNull Block blockUpdate(@NotNull UpdateState updateState) {
        return genericUpdateState(updateState.instance(), updateState.currentBlock(), updateState.blockPosition(), updateState.fromFace());
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        return genericUpdateState(placementState.instance(), block, placementState.placePosition(), null);
    }

    private @NotNull Block genericUpdateState(@NotNull Block.Getter instance, @NotNull Block block, @NotNull Point blockPosition, @Nullable BlockFace faceFilter) {
        for (var blockFace : HORIZONTAL_FACES) {
            if (faceFilter != null && blockFace != faceFilter) continue;
            var neighbor = instance.getBlock(blockPosition.relative(blockFace));

            var neighborFaceIsSolid = neighbor.registry().collisionShape().isOccluded(block.registry().collisionShape(), blockFace.getOppositeFace());
            var canConnect = canConnect(neighbor) && (neighborFaceIsSolid || isSimilarFence(block, neighbor)
                    || FenceGatePlacementRule.isConnectableGate(neighbor, blockFace));
            block = block.withProperty(blockFace.name().toLowerCase(), String.valueOf(canConnect));
        }
        return block;
    }

    private boolean isSimilarFence(@NotNull Block block, @NotNull Block neighbor) {
        return BlockTags.FENCES.contains(neighbor.namespace()) &&
                BlockTags.WOODEN_FENCES.contains(block.namespace()) == BlockTags.WOODEN_FENCES.contains(neighbor.namespace());
    }

    @Override
    public int maxUpdateDistance() {
        return 1;
    }
}
