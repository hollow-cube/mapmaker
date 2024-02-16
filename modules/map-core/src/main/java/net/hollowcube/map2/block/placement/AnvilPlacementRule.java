package net.hollowcube.map2.block.placement;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Objects;

// Its like facing horizontal but rotated 90 degrees
public class AnvilPlacementRule extends BaseBlockPlacementRule {

    public AnvilPlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @UnknownNullability Block blockPlace(@NotNull PlacementState placementState) {
        var playerPosition = Objects.requireNonNullElse(placementState.playerPosition(), Pos.ZERO);
        var facing = rotate(BlockFace.fromYaw(playerPosition.yaw()));
        return block.withProperty("facing", facing.name().toLowerCase());
    }

    private static @NotNull BlockFace rotate(@NotNull BlockFace face) {
        return switch (face) {
            case NORTH -> BlockFace.EAST;
            case EAST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.WEST;
            case WEST -> BlockFace.NORTH;
            default -> face;
        };
    }
}
