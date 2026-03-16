package net.hollowcube.mapmaker.map.block.placement;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Objects;

// Its like facing horizontal but rotated 90 degrees
public class AnvilPlacementRule extends BaseBlockPlacementRule {

    public AnvilPlacementRule(Block block) {
        super(block);
    }

    @Override
    public @UnknownNullability Block blockPlace(PlacementState placementState) {
        var playerPosition = Objects.requireNonNullElse(placementState.playerPosition(), Pos.ZERO);
        var facing = rotate(BlockFace.fromYaw(playerPosition.yaw()));
        return block.withProperty("facing", facing.name().toLowerCase());
    }

    private static BlockFace rotate(BlockFace face) {
        return switch (face) {
            case NORTH -> BlockFace.EAST;
            case EAST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.WEST;
            case WEST -> BlockFace.NORTH;
            default -> face;
        };
    }
}
