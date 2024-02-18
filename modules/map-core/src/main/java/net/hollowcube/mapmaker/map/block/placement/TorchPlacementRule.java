package net.hollowcube.mapmaker.map.block.placement;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
public class TorchPlacementRule extends BaseBlockPlacementRule {
    private static final List<BlockFace> HORIZONTAL_FACES = List.of(
            BlockFace.NORTH,
            BlockFace.WEST,
            BlockFace.SOUTH,
            BlockFace.EAST
    );

    private static final String PROP_FACING = "facing";

    private final Block wallBlock;

    public TorchPlacementRule(@NotNull Block torchBlock, @NotNull Block wallBlock) {
        super(torchBlock);
        this.wallBlock = wallBlock;
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        var blockFace = Objects.requireNonNullElse(placementState.blockFace(), BlockFace.BOTTOM);
        return switch (blockFace) {
            case TOP -> block;
            case NORTH, SOUTH, EAST, WEST -> wallBlock.withProperty(PROP_FACING, blockFace.name().toLowerCase());
            case BOTTOM -> {
                var instance = placementState.instance();

                var posBelow = placementState.placePosition().sub(0, 1, 0);
                if (instance.getBlock(posBelow, Block.Getter.Condition.TYPE).isSolid()) {
                    yield block;
                }

                var playerPosition = Objects.requireNonNullElse(placementState.playerPosition(), Pos.ZERO);
                var facingFace = BlockFace.fromYaw(playerPosition.yaw());
                for (var neighborFace : getFaceOrder(facingFace)) {
                    var neighbor = instance.getBlock(placementState.placePosition().relative(neighborFace));
                    if (neighbor.isSolid()) {
                        yield wallBlock.withProperty(PROP_FACING, neighborFace.getOppositeFace().name().toLowerCase());
                    }
                }

                yield null;
            }
        };
    }

    private @NotNull BlockFace[] getFaceOrder(@NotNull BlockFace facingFace) {
        return new BlockFace[]{
                facingFace, // Front
                HORIZONTAL_FACES.get((HORIZONTAL_FACES.indexOf(facingFace) + 3) % HORIZONTAL_FACES.size()), // CW
                HORIZONTAL_FACES.get((HORIZONTAL_FACES.indexOf(facingFace) + 1) % HORIZONTAL_FACES.size()), // CCW
                facingFace.getOppositeFace(), // Opposite
        };
    }

}
