package net.hollowcube.map.block.rule;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ButtonPlacementRule extends BaseBlockPlacementRule {
    private static final String PROP_FACE = "face";
    private static final String PROP_FACING = "facing";

    public ButtonPlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        var blockFace = Objects.requireNonNullElse(placementState.blockFace(), BlockFace.WEST);
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
