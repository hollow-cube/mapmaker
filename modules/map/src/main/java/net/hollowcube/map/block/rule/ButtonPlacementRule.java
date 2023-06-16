package net.hollowcube.map.block.rule;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ButtonPlacementRule extends BlockPlacementRule {
    private static final String PROP_FACE = "face";
    private static final String PROP_FACING = "facing";

    public ButtonPlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @NotNull Block blockUpdate(@NotNull UpdateState updateState) {
        return updateState.currentBlock();
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        var blockFace = placementState.blockFace();
        return switch (blockFace) {
            case NORTH, SOUTH, EAST, WEST -> block
                    .withProperty(PROP_FACE, "wall")
                    .withProperty(PROP_FACING, blockFace.name().toLowerCase());
            case TOP, BOTTOM -> {
                var facingFace = BlockFace.fromYaw(placementState.playerPosition().yaw()).name().toLowerCase();
                yield block.withProperty(PROP_FACE, blockFace == BlockFace.TOP ? "floor" : "ceiling")
                        .withProperty(PROP_FACING, facingFace);
            }
        };
    }
}
