package net.hollowcube.schem.old;

import net.hollowcube.schem.Rotation;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class CoordinateUtil {
    private CoordinateUtil() {
    }

    public static int blockIndex(@NotNull Point size, @NotNull Point pos) {
        return blockIndex(size, pos.blockX(), pos.blockY(), pos.blockZ());
    }

    public static int blockIndex(@NotNull Point size, int x, int y, int z) {
        return (int) (x + z * size.x() + y * size.x() * size.z());
    }

    public static @NotNull Point floor(@NotNull Point point) {
        return new Vec(point.blockX(), point.blockY(), point.blockZ());
    }

    public static @NotNull Point abs(@NotNull Point point) {
        return new Vec(Math.abs(point.x()), Math.abs(point.y()), Math.abs(point.z()));
    }

    public static @NotNull Point min(@NotNull Point a, @NotNull Point b) {
        return new Vec(
                Math.min(a.x(), b.x()),
                Math.min(a.y(), b.y()),
                Math.min(a.z(), b.z())
        );
    }

    public static @NotNull Point max(@NotNull Point a, @NotNull Point b) {
        return new Vec(
                Math.max(a.x(), b.x()),
                Math.max(a.y(), b.y()),
                Math.max(a.z(), b.z())
        );
    }

    public static @NotNull Point rotatePos(@NotNull Point point, @NotNull Rotation rotation) {
        return switch (rotation) {
            case NONE -> point;
            case CLOCKWISE_90 -> noNegativeZero(new Vec(-point.z(), point.y(), point.x()));
            case CLOCKWISE_180 -> noNegativeZero(new Vec(-point.x(), point.y(), -point.z()));
            case CLOCKWISE_270 -> noNegativeZero(new Vec(point.z(), point.y(), -point.x()));
        };
    }

    private static @NotNull Point noNegativeZero(@NotNull Point point) {
        return new Vec(
                point.x() == 0 ? 0 : point.x(),
                point.y() == 0 ? 0 : point.y(),
                point.z() == 0 ? 0 : point.z()
        );
    }

    public static @NotNull Block rotateBlock(@NotNull Block block, @NotNull Rotation rotation) {
        if (rotation == Rotation.NONE) return block;

        Block newBlock = block;

        if (block.getProperty("facing") != null) {
            newBlock = rotateFacing(block, rotation);
        }
        if (block.getProperty("north") != null) {
            newBlock = rotateFence(block, rotation);
        }

        return newBlock;
    }

    /**
     * Rotates blocks that have a "facing" property
     */
    private static Block rotateFacing(Block block, Rotation rotation) {
        return switch (rotation) {
            case NONE -> block;
            case CLOCKWISE_90 -> block.withProperty("facing",
                    rotate90(block.getProperty("facing")));
            case CLOCKWISE_180 -> block.withProperty("facing",
                    rotate90(rotate90(block.getProperty("facing"))));
            case CLOCKWISE_270 -> block.withProperty("facing",
                    rotate90(rotate90(rotate90(block.getProperty("facing")))));
        };
    }

    /**
     * Rotates fences, walls and glass panes
     * (blocks that have "north" "east" "south" "west" properties)
     */
    private static Block rotateFence(Block block, Rotation rotation) {
        return switch (rotation) {
            case NONE -> block;
            case CLOCKWISE_90 -> block.withProperties(Map.of(
                    "north", block.getProperty("west"),
                    "east", block.getProperty("north"),
                    "south", block.getProperty("east"),
                    "west", block.getProperty("south")
            ));
            case CLOCKWISE_180 -> block.withProperties(Map.of(
                    "north", block.getProperty("south"),
                    "east", block.getProperty("west"),
                    "south", block.getProperty("north"),
                    "west", block.getProperty("east")
            ));
            case CLOCKWISE_270 -> block.withProperties(Map.of(
                    "north", block.getProperty("east"),
                    "east", block.getProperty("south"),
                    "south", block.getProperty("west"),
                    "west", block.getProperty("north")
            ));
        };
    }

    private static String rotate90(String in) {
        return switch (in) {
            case "north" -> "east";
            case "east" -> "south";
            case "south" -> "west";
            case "west" -> "north";
            default -> in;
        };
    }

}
