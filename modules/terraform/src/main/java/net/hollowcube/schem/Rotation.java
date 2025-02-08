package net.hollowcube.schem;

import net.hollowcube.common.types.Axis;
import net.minestom.server.utils.Direction;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Represents a 90 degree rotation around the Y axis.
 */
public enum Rotation {
    NONE,
    CLOCKWISE_90,
    CLOCKWISE_180,
    CLOCKWISE_270;

    public int toDegrees() {
        return ordinal() * 90;
    }

    /**
     * Converts a Minestom {@link net.minestom.server.utils.Rotation} to a rotation usable in a schematic.
     * <p>
     * Minestom rotation supports 45 degree angles, if passed to this function they will be rounded down to the nearest 90 degree angle.
     */
    public static @NotNull Rotation from(@NotNull net.minestom.server.utils.Rotation rotation) {
        return values()[rotation.ordinal() / 2];
    }

    public @NotNull Rotation rotate(@NotNull Rotation rotation) {
        return values()[(ordinal() + rotation.ordinal()) % 4];
    }

    public @NotNull Direction rotate(@NotNull Direction input) {
        return switch (this) {
            case NONE -> input;
            case CLOCKWISE_90 -> switch (input) {
                case NORTH -> Direction.EAST;
                case EAST -> Direction.SOUTH;
                case SOUTH -> Direction.WEST;
                case WEST -> Direction.NORTH;
                default -> input;
            };
            case CLOCKWISE_180 -> switch (input) {
                case NORTH -> Direction.SOUTH;
                case EAST -> Direction.WEST;
                case SOUTH -> Direction.NORTH;
                case WEST -> Direction.EAST;
                default -> input;
            };
            case CLOCKWISE_270 -> switch (input) {
                case NORTH -> Direction.WEST;
                case EAST -> Direction.NORTH;
                case SOUTH -> Direction.EAST;
                case WEST -> Direction.SOUTH;
                default -> input;
            };
        };
    }

    public @NotNull Axis rotateAroundY(@NotNull Axis axis) {
        return switch (this) {
            case NONE, CLOCKWISE_180 -> axis;
            case CLOCKWISE_90, CLOCKWISE_270 -> switch (axis) {
                case X -> Axis.Z;
                case Z -> Axis.X;
                default -> axis;
            };
        };
    }

    public int rotate(@NotNull Integer rotation) {
        return (rotation + this.ordinal() * 4) % 16;
    }

    public @NotNull Map<Direction, String> rotate(@NotNull Map<Direction, String> connections) {
        return switch (this) {
            case NONE -> connections;
            case CLOCKWISE_90 -> Map.of(
                    Direction.NORTH, connections.get(Direction.WEST),
                    Direction.EAST, connections.get(Direction.NORTH),
                    Direction.SOUTH, connections.get(Direction.EAST),
                    Direction.WEST, connections.get(Direction.SOUTH)
            );
            case CLOCKWISE_180 -> Map.of(
                    Direction.NORTH, connections.get(Direction.SOUTH),
                    Direction.EAST, connections.get(Direction.WEST),
                    Direction.SOUTH, connections.get(Direction.NORTH),
                    Direction.WEST, connections.get(Direction.EAST)
            );
            case CLOCKWISE_270 -> Map.of(
                    Direction.NORTH, connections.get(Direction.EAST),
                    Direction.EAST, connections.get(Direction.SOUTH),
                    Direction.SOUTH, connections.get(Direction.WEST),
                    Direction.WEST, connections.get(Direction.NORTH)
            );
        };
    }
}
