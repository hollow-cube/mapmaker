package net.hollowcube.map.block.placement;

import net.hollowcube.map.block.BlockTags;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
public class PanePlacementRule extends BaseBlockPlacementRule {
    private static final BlockFace[] HORIZONTAL_FACES = new BlockFace[]{
            BlockFace.NORTH,
            BlockFace.SOUTH,
            BlockFace.EAST,
            BlockFace.WEST
    };

    public PanePlacementRule(@NotNull Block block) {
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
            var canConnect = canConnect(neighbor) && (neighborFaceIsSolid || BlockTags.GLASS_PANES.contains(neighbor.namespace()) || BlockTags.WALLS.contains(neighbor.namespace()) || neighbor.id() == Block.IRON_BARS.id());
            block = block.withProperty(blockFace.name().toLowerCase(), String.valueOf(canConnect));
        }
        return block;
    }

    @Override
    public int maxUpdateDistance() {
        return 1;
    }
}
