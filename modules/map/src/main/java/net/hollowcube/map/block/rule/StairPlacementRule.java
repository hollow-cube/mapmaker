package net.hollowcube.map.block.rule;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class StairPlacementRule extends BlockPlacementRule {
    private static final BlockFace[][] HORIZONTAL_FACING = new BlockFace[][]{
            // indices here are blockface.ordinal - 2
            /* NORTH */{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST},
            /* SOUTH */{BlockFace.SOUTH, BlockFace.WEST, BlockFace.NORTH, BlockFace.EAST},
            /* WEST  */{BlockFace.WEST, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH},
            /* EAST  */{BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.NORTH},
    };

    private static final String PROP_FACING = "facing";
    private static final String PROP_HALF = "half";
    private static final String PROP_SHAPE = "shape";

    public StairPlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @NotNull Block blockUpdate(@NotNull UpdateState updateState) {
        var block = updateState.currentBlock();

        var facing = BlockFace.valueOf(block.getProperty(PROP_FACING).toUpperCase());

        var instance = updateState.instance();
        var blockPos = updateState.blockPosition();

        // Reordered directions for the side array. See comment in that function
        // for explanation of the order here.
        var sides = new int[4];
        var orderedFaces = HORIZONTAL_FACING[facing.ordinal() - 2];
        for (int i = 0; i < 4; i++) {
            var blockFace = orderedFaces[i];
            var relativeBlock = instance.getBlock(blockPos.relative(blockFace), Block.Getter.Condition.TYPE);
            if (!BlockTags.MINECRAFT_STAIRS.contains(relativeBlock.namespace())) continue; // Non-stairs never connect

            var relativeFacing = BlockFace.valueOf(relativeBlock.getProperty(PROP_FACING).toUpperCase());
            if (facing.isSimilar(blockFace)) {
                // If it is a face next to the stair, then the rule is:
                // canConnect = rel.facing is perpendicular to block.facing
                var canConnect = !relativeFacing.isSimilar(facing);
                if (canConnect) {
                    var nextFace = orderedFaces[(i + 1) % 4];
                    sides[i] = nextFace.equals(relativeFacing) ? 2 : 1;
                }
            } else {
                // If it is a face opposite to the stair, then the rule is:
                // canConnect = rel.facing == block.facing || rel.facing == blockFace
                //todo this should also allow opposite of relative facing i think?
                var canConnect = relativeFacing.equals(facing) || relativeFacing.equals(blockFace);
                // Weird edge case in vanilla (Issue #108)
                if ((block.getProperty(PROP_SHAPE).equals("outer_right") ||
                        block.getProperty(PROP_SHAPE).equals("outer_left")) &&
                        !relativeFacing.equals(facing))
                    canConnect = false;
                if (canConnect) sides[i] = 1;
            }
        }

        return block.withProperty(PROP_SHAPE, parseShapeFromSides(sides));
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        var placeFace = placementState.blockFace();
        var placeY = placementState.cursorPosition().y();
        var half = placeFace == BlockFace.TOP || (placeFace != BlockFace.BOTTOM && placeY < 0.5) ? "bottom" : "top";

        // Facing is always the player facing direction, and is never updated
        var facing = BlockFace.fromYaw(placementState.playerPosition().yaw());

        return block.withProperties(Map.of(
                PROP_HALF, half,
                PROP_FACING, facing.name().toLowerCase()
        ));
    }

    private static final String[] SHAPE_INDICES = new String[]{
            "straight", "straight", "inner_right", "inner_left", "inner_right", "straight",
            "straight", "straight", "straight", "inner_left", "straight", "straight",
            "outer_left", "outer_left", "outer_left", "outer_left", "outer_left", "outer_left",
            "straight", "straight", "straight", "inner_left", "straight", "straight",
            "outer_right", "straight", "outer_right", "outer_right", "inner_right", "straight",
            "outer_right", "straight", "outer_right", "outer_right", "straight", "straight",
    };

    private static @NotNull String parseShapeFromSides(int[] sides) {
        // Sides are a 4 element array with a value of whether the stair may connect to
        // that block, and how. The order of the elements is clockwise from the
        // "perspective" of the stair block. For example, if the stair block is facing
        // north, then the array will be "north east south west".
        // The numbers in the array are as follows:
        // - 0: No connection
        // - 1: Connection to the side, right
        // - 2: Connection to the side, left
        // Those are only relevant for faces parallel to the stair.
        int index = (sides[0] * 12) + (sides[1] * 6) + (sides[2] == 0 ? 0 : sides[2] + sides[3] + 1) + sides[3];
        return SHAPE_INDICES[index];
    }
}
