package net.hollowcube.terraform.util.math;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.utils.Direction;
import org.jetbrains.annotations.NotNull;

public final class DirectionUtil {


    public static @NotNull Direction fromView(@NotNull Pos pos) {
        float pitch = pos.pitch(), yaw = pos.yaw();
        if (pitch <= -45) {
            return Direction.UP;
        } else if (pitch >= 45) {
            return Direction.DOWN;
        } else if (yaw >= -45 && yaw < 45) {
            return Direction.SOUTH;
        } else if (yaw >= 45 && yaw < 135) {
            return Direction.WEST;
        } else if (yaw >= -135 && yaw < -45) {
            return Direction.EAST;
        } else {
            return Direction.NORTH;
        }
    }

    public static @NotNull Direction fromYaw(@NotNull Pos pos) {
        float yaw = pos.yaw();
        if (yaw >= -45 && yaw < 45) {
            return Direction.SOUTH;
        } else if (yaw >= 45 && yaw < 135) {
            return Direction.WEST;
        } else if (yaw >= -135 && yaw < -45) {
            return Direction.EAST;
        } else {
            return Direction.NORTH;
        }
    }

    public static @NotNull Direction rotate(@NotNull Direction direction, boolean clockwise) {
        return switch (direction) {
            case NORTH -> clockwise ? Direction.EAST : Direction.WEST;
            case EAST -> clockwise ? Direction.SOUTH : Direction.NORTH;
            case SOUTH -> clockwise ? Direction.WEST : Direction.EAST;
            case WEST -> clockwise ? Direction.NORTH : Direction.SOUTH;
            default -> direction;
        };
    }

    public static boolean isPositive(@NotNull Direction direction) {
        return direction.normalX() > 0 || direction.normalY() > 0 || direction.normalZ() > 0;
    }

    private DirectionUtil() {
    }

}
