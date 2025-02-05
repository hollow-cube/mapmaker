package net.hollowcube.mapmaker.map.block.placement;

import net.hollowcube.terraform.util.math.DirectionUtil;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.utils.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

public class DoorPlacementRule extends BaseBlockPlacementRule {
    public DoorPlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        var instance = placementState.instance();
        var blockPosition = placementState.placePosition();

        var abovePosition = blockPosition.add(0, 1, 0);
        if (!instance.getBlock(abovePosition, Block.Getter.Condition.TYPE).registry().isReplaceable())
            return null;

        var playerPosition = Objects.requireNonNullElse(placementState.playerPosition(), Pos.ZERO);
        var facing = BlockFace.fromYaw(playerPosition.yaw());
        var isLeftHinge = computeHinge(instance, blockPosition, facing.toDirection(), placementState.cursorPosition());
        var placedBlock = block.withProperties(Map.of(
                "facing", facing.name().toLowerCase(),
                "hinge", isLeftHinge ? "left" : "right")
        );

        var aboveBlock = placedBlock.withProperty("half", "upper");
        placeOtherBlock(instance, abovePosition, aboveBlock);

        return placedBlock;
    }

    private boolean computeHinge(@NotNull Block.Getter instance, @NotNull Point position, @NotNull Direction direction, @Nullable Point cursor) {
        var left = position.relative(BlockFace.fromDirection(DirectionUtil.rotate(direction, false)));
        var right = position.relative(BlockFace.fromDirection(DirectionUtil.rotate(direction, true)));
        var leftBlock = instance.getBlock(left);
        var rightBlock = instance.getBlock(right);
        if (Objects.equals(leftBlock.getProperty("hinge"), "left")) return false;
        if (Objects.equals(rightBlock.getProperty("hinge"), "right")) return true;
        if (cursor == null) return false;

        return switch (direction) {
            case NORTH -> cursor.x() < 0.5;
            case SOUTH -> cursor.x() > 0.5;
            case WEST -> cursor.z() > 0.5;
            case EAST -> cursor.z() < 0.5;
            default -> false;
        };
    }
}
