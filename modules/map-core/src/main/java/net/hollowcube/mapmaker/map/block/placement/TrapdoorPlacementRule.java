package net.hollowcube.mapmaker.map.block.placement;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class TrapdoorPlacementRule extends BaseBlockPlacementRule {
    private static final String PROP_HALF = "half";
    private static final String PROP_FACING = "facing";

    public TrapdoorPlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        var placeFace = Objects.requireNonNullElse(placementState.blockFace(), BlockFace.TOP);
        return switch (placeFace) {
            case NORTH, SOUTH, EAST, WEST -> {
                var cursorPosition = Objects.requireNonNullElse(placementState.cursorPosition(), Vec.ZERO);
                var half = cursorPosition.y() > 0.5 ? "top" : "bottom";
                var facing = placeFace.name().toLowerCase();
                yield block.withProperty(PROP_HALF, half).withProperty(PROP_FACING, facing);
            }
            case TOP, BOTTOM -> {
                var half = placeFace.getOppositeFace().name().toLowerCase();
                var playerPosition = Objects.requireNonNullElse(placementState.playerPosition(), Pos.ZERO);
                var facing = BlockFace.fromYaw(playerPosition.yaw())
                        .getOppositeFace().name().toLowerCase();
                yield block.withProperty(PROP_HALF, half).withProperty(PROP_FACING, facing);
            }
        };
    }
}
