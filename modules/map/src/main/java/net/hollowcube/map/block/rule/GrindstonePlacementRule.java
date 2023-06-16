package net.hollowcube.map.block.rule;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GrindstonePlacementRule extends BlockPlacementRule {
    private static final String PROP_FACING = "facing";
    private static final String PROP_FACE = "face"; // wall/floor/ceiling

    public GrindstonePlacementRule() {
        super(Block.GRINDSTONE);
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        var placeFace = placementState.blockFace();
        var facing = BlockFace.fromYaw(placementState.playerPosition().yaw());
        return switch (placeFace) {
            case NORTH, SOUTH, EAST, WEST -> this.block
                    .withProperty(PROP_FACE, "wall")
                    .withProperty(PROP_FACING, placeFace.name().toLowerCase());
            case TOP, BOTTOM -> this.block
                    .withProperty(PROP_FACE, placeFace == BlockFace.TOP ? "floor" : "ceiling")
                    .withProperty(PROP_FACING, facing.name().toLowerCase());
        };
    }
}
