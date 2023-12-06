package net.hollowcube.map.block.placement;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class StandingSignPlacementRule extends BaseBlockPlacementRule {
    private static final List<BlockFace> HORIZONTAL_FACES = List.of(
            BlockFace.NORTH,
            BlockFace.WEST,
            BlockFace.SOUTH,
            BlockFace.EAST
    );

    private static final String PROP_FACING = "facing";

    private final Block wallBlock;

    public StandingSignPlacementRule(@NotNull Block block) {
        super(block);

        var wallBlockId = String.format("minecraft:%s_wall_sign", block.namespace().path().replace("_sign", ""));
        this.wallBlock = Objects.requireNonNull(Block.fromNamespaceId(wallBlockId))
                .withNbt(block.nbt()).withHandler(block.handler());
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        var blockFace = Objects.requireNonNullElse(placementState.blockFace(), BlockFace.TOP);
        var playerPosition = Objects.requireNonNullElse(placementState.playerPosition(), Pos.ZERO);
        return switch (blockFace) {
            case NORTH, SOUTH, EAST, WEST -> wallBlock.withProperty(PROP_FACING, blockFace.name().toLowerCase());
            case BOTTOM -> {
                var instance = placementState.instance();

                var facingFace = BlockFace.fromYaw(playerPosition.yaw());
                for (var neighborFace : getFaceOrder(facingFace)) {
                    var neighbor = instance.getBlock(placementState.placePosition().relative(neighborFace));
                    if (neighbor.isSolid()) {
                        yield wallBlock.withProperty(PROP_FACING, neighborFace.getOppositeFace().name().toLowerCase());
                    }
                }

                yield null;
            }
            case TOP -> {
                float yaw = playerPosition.yaw() + 180;
                int rotation = (int) (Math.round(yaw / 22.5d) % 16);
                yield block.withProperty("rotation", String.valueOf(rotation));
            }
        };
    }

    private @NotNull BlockFace[] getFaceOrder(@NotNull BlockFace facingFace) {
        return new BlockFace[]{
                facingFace, // Front
                HORIZONTAL_FACES.get((HORIZONTAL_FACES.indexOf(facingFace) + 1) % HORIZONTAL_FACES.size()), // CW
                HORIZONTAL_FACES.get((HORIZONTAL_FACES.indexOf(facingFace) + 3) % HORIZONTAL_FACES.size()), // CCW
                facingFace.getOppositeFace(), // Opposite
        };
    }
}
