package net.hollowcube.map.block.rule;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class GrindstonePlacementRule extends BaseBlockPlacementRule {
    private static final String PROP_FACING = "facing";
    private static final String PROP_FACE = "face"; // wall/floor/ceiling

    public GrindstonePlacementRule() {
        super(Block.GRINDSTONE);
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        var placeFace = Objects.requireNonNullElse(placementState.blockFace(), BlockFace.TOP);
        var playerPosition = Objects.requireNonNullElse(placementState.playerPosition(), Pos.ZERO);
        return switch (placeFace) {
            case NORTH, SOUTH, EAST, WEST -> this.block
                    .withProperty(PROP_FACE, "wall")
                    .withProperty(PROP_FACING, placeFace.name().toLowerCase());
            case TOP, BOTTOM -> this.block
                    .withProperty(PROP_FACE, placeFace == BlockFace.TOP ? "floor" : "ceiling")
                    .withProperty(PROP_FACING, BlockFace.fromYaw(playerPosition.yaw()).name().toLowerCase());
        };
    }
}
