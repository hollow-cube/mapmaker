package net.hollowcube.map.block.placement;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class FacingClickHorizontalPlacementRule extends BaseBlockPlacementRule {
    private static final List<BlockFace> HORIZONTAL_FACES = List.of(
            BlockFace.NORTH,
            BlockFace.WEST,
            BlockFace.SOUTH,
            BlockFace.EAST
    );

    private static final String PROP_FACING = "facing";

    public FacingClickHorizontalPlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        var blockFace = Objects.requireNonNullElse(placementState.blockFace(), BlockFace.TOP);
        return switch (blockFace) {
            case NORTH, SOUTH, EAST, WEST -> block.withProperty(PROP_FACING, blockFace.name().toLowerCase());
            case TOP, BOTTOM -> {
                var implicitFace = getImplicitFace(placementState);
                yield implicitFace == null ? null : block.withProperty(PROP_FACING, implicitFace.name().toLowerCase());
            }
        };
    }

    // Computes the horizontal face "closest" to the player in case they clicked the top or bottom face.
    // Returns null if there are no solid blocks to place against
    static @Nullable BlockFace getImplicitFace(@NotNull PlacementState placement) {
        var instance = placement.instance();
        var playerPosition = Objects.requireNonNullElse(placement.playerPosition(), Pos.ZERO);
        var facingFace = BlockFace.fromYaw(playerPosition.yaw());
        for (var neighborFace : getFaceOrder(facingFace)) {
            var neighbor = instance.getBlock(placement.placePosition().relative(neighborFace));
            if (neighbor.isSolid()) {
                return neighborFace.getOppositeFace();
            }
        }
        return null;
    }

    private static @NotNull BlockFace[] getFaceOrder(@NotNull BlockFace facingFace) {
        return new BlockFace[]{
                facingFace, // Front
                HORIZONTAL_FACES.get((HORIZONTAL_FACES.indexOf(facingFace) + 1) % HORIZONTAL_FACES.size()), // CW
                HORIZONTAL_FACES.get((HORIZONTAL_FACES.indexOf(facingFace) + 3) % HORIZONTAL_FACES.size()), // CCW
                facingFace.getOppositeFace(), // Opposite
        };
    }

}
