package net.hollowcube.map.block.rule;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TrapdoorPlacementRule extends BlockPlacementRule {
    private static final String PROP_HALF = "half";
    private static final String PROP_FACING = "facing";

    public TrapdoorPlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        var placeFace = placementState.blockFace();
        return switch (placeFace) {
            case NORTH, SOUTH, EAST, WEST -> {
                var half = placementState.cursorPosition().y() > 0.5 ? "top" : "bottom";
                var facing = placeFace.name().toLowerCase();
                yield block.withProperty(PROP_HALF, half).withProperty(PROP_FACING, facing);
            }
            case TOP, BOTTOM -> {
                var half = placeFace.getOppositeFace().name().toLowerCase();
                var facing = BlockFace.fromYaw(placementState.playerPosition().yaw())
                        .getOppositeFace().name().toLowerCase();
                yield block.withProperty(PROP_HALF, half).withProperty(PROP_FACING, facing);
            }
        };
    }
}
