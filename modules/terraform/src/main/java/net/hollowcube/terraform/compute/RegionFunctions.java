package net.hollowcube.terraform.compute;

import net.hollowcube.terraform.buffer.BlockBuffer;
import net.hollowcube.terraform.mask.Mask;
import net.hollowcube.terraform.pattern.Pattern;
import net.hollowcube.terraform.selection.region.CuboidRegion;
import net.hollowcube.terraform.selection.region.Region;
import net.hollowcube.terraform.task.ComputeFunc;
import net.hollowcube.terraform.task.edit.WorldView;
import net.hollowcube.terraform.util.math.DirectionUtil;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.*;

public final class RegionFunctions {

    private RegionFunctions() {
    }

    public static @NotNull ComputeFunc replace(@NotNull Region region, @NotNull Mask mask, @NotNull Pattern pattern) {
        return (_, world) -> {
            var buffer = BlockBuffer.builder(world, region.min(), region.max());
            for (var pos : region) {
                var block = world.getBlock(pos);
                if (!mask.test(world, pos, block)) continue;
                setBlock(buffer, world, pos, pattern);
                //todo block entities
            }
            return buffer.build();
        };
    }

    public static @NotNull ComputeFunc cuboid(@NotNull CuboidRegion region, @NotNull Pattern pattern, boolean hollow, Direction... faces) {
        return cuboid(region, pattern, hollow, Mask.always(), faces);
    }

    public static @NotNull ComputeFunc cuboid(@NotNull CuboidRegion region, @NotNull Pattern pattern, boolean hollow, @NotNull Mask mask, Direction... faces) {
        var faceSet = faces.length > 0 ? EnumSet.copyOf(List.of(faces)) : EnumSet.allOf(Direction.class);

        return (_, world) -> {
            var buffer = BlockBuffer.builder(world, region.min(), region.max());

            for (int x = region.min().blockX(); x < region.max().blockX(); x++) {
                for (int y = region.min().blockY(); y < region.max().blockY(); y++) {
                    for (int z = region.min().blockZ(); z < region.max().blockZ(); z++) {
                        var pos = new Vec(x, y, z);

                        if (!mask.test(world, pos, world.getBlock(pos))) {
                            continue;
                        }

                        // If not hollow always set the block
                        if (!hollow) {
                            setBlock(buffer, world, pos, pattern);
                            continue;
                        }

                        // Otherwise we only set on particular faces
                        if (faceSet.contains(Direction.UP) && y == region.max().blockY() - 1) {
                            setBlock(buffer, world, pos, pattern);
                        } else if (faceSet.contains(Direction.DOWN) && y == region.min().blockY()) {
                            setBlock(buffer, world, pos, pattern);
                        } else if (faceSet.contains(Direction.NORTH) && z == region.min().blockZ()) {
                            setBlock(buffer, world, pos, pattern);
                        } else if (faceSet.contains(Direction.SOUTH) && z == region.max().blockZ() - 1) {
                            setBlock(buffer, world, pos, pattern);
                        } else if (faceSet.contains(Direction.WEST) && x == region.min().blockX()) {
                            setBlock(buffer, world, pos, pattern);
                        } else if (faceSet.contains(Direction.EAST) && x == region.max().blockX() - 1) {
                            setBlock(buffer, world, pos, pattern);
                        }
                    }
                }
            }

            return buffer.build();
        };
    }

    public static @NotNull ComputeFunc stack(@NotNull Region region, @NotNull Direction direction, int count) {
        return stack(region, direction, count, Mask.always());
    }

    public static @NotNull ComputeFunc stack(@NotNull Region region, @NotNull Direction direction, int count, @NotNull Mask mask) {
        var offset = new Vec(direction.normalX(), direction.normalY(), direction.normalZ())
                // Multiply by the region size to get the offset in the direction
                // Works because direction will be (0 1 0), so only the height is affected
                .mul(region.max().sub(region.min()));

        return (_, world) -> {
            var min = DirectionUtil.isPositive(direction) ? region.min() : region.min().add(offset.mul(count));
            var max = DirectionUtil.isPositive(direction) ? region.max().add(offset.mul(count)) : region.max();
            var buffer = BlockBuffer.builder(world, min, max);
            for (int i = 0; i < count; i++) {
                var blockOffset = offset.mul(i + 1);

                // Copy every block to offset
                for (var pos : region) {
                    var block = world.getBlock(pos, Block.Getter.Condition.TYPE);
                    if (!mask.test(world, pos, block)) continue;

                    buffer.set(pos.add(blockOffset), block);
                }
            }
            return buffer.build();
        };
    }

    public static @NotNull ComputeFunc move(@NotNull Region region, @NotNull Direction direction, int distance) {
        return move(region, direction, distance, Pattern.air(), Mask.always());
    }

    public static @NotNull ComputeFunc move(@NotNull Region region, @NotNull Direction direction, int distance, @NotNull Pattern replacement, @NotNull Mask mask) {
        var offset = new Vec(direction.normalX(), direction.normalY(), direction.normalZ()).mul(distance);

        //todo: this can be turned into a single pass if the iteration direction is changed based on what direction
        // the move is happening. Not sure its worth doing, but might be a good optimization.
        return (_, world) -> {
            var min = DirectionUtil.isPositive(direction) ? region.min() : region.min().add(offset);
            var max = DirectionUtil.isPositive(direction) ? region.max().add(offset) : region.max();
            var buffer = BlockBuffer.builder(world, min, max);

            // First pass, remove the old blocks (replacing with replacement pattern)
            // We do this first in order to not overwrite our valid blocks with air
            for (var pos : region) {
                Block block = world.getBlock(pos);
                if (!mask.test(world, pos, block)) continue;

                setBlock(buffer, world, pos, replacement);
            }

            // Second pass, add the moved blocks
            for (var pos : region) {
                Block block = world.getBlock(pos);
                if (!mask.test(world, pos, block)) continue;

                buffer.set(pos.add(offset), block);
            }

            return buffer.build();
        };
    }

    public static @NotNull ComputeFunc line(@NotNull Point p1, @NotNull Point p2, @NotNull Pattern pattern) {
        return line(p1, p2, pattern, Mask.always());
    }

    public static @NotNull ComputeFunc line(@NotNull Point pos1, @NotNull Point pos2, @NotNull Pattern pattern, @NotNull Mask mask) {
        return (_, world) -> {
            var builder = BlockBuffer.builder(world, pos1, pos2);

            line(builder, world, pos1, pos2, pattern, mask, 5);

            return builder.build();
        };
    }

    private static void line(
            @NotNull BlockBuffer.Builder builder,
            @NotNull WorldView world,
            @NotNull Point pos1,
            @NotNull Point pos2,
            @NotNull Pattern pattern,
            @NotNull Mask mask,
            @Range(from = 1, to = 10) int steps
    ) throws InterruptedException {
        var line = Vec.fromPoint(pos2.sub(pos1));

        var step = line.normalize().div(steps);
        var current = Vec.ZERO;

        var points = new HashSet<Point>();

        while (current.length() < line.length()) {
            points.add(new BlockVec(pos1.add(current)));
            current = current.add(step);
        }

        for (var point : points) {
            if (!mask.test(world, point, world.getBlock(point))) continue;
            setBlock(builder, world, point, pattern);
        }
    }

    public static @NotNull ComputeFunc outline(@NotNull CuboidRegion region, @NotNull Pattern pattern) {
        return outline(region, pattern, Mask.always());
    }

    public static @NotNull ComputeFunc outline(@NotNull CuboidRegion region, @NotNull Pattern pattern, @NotNull Mask mask) {
        return (_, world) -> {
            var builder = BlockBuffer.builder(world, region.min(), region.max());
            Point max = region.max().sub(1,1,1), min = region.min();

            line(builder, world, min, min.withX(max.x() + 1), pattern, mask, 1);
            line(builder, world, min, min.withY(max.y() + 1), pattern, mask, 1);
            line(builder, world, min, min.withZ(max.z() + 1), pattern, mask, 1);
            line(builder, world, max, max.withX(min.x() - 1), pattern, mask, 1);
            line(builder, world, max, max.withY(min.y() - 1), pattern, mask, 1);
            line(builder, world, max, max.withZ(min.z() - 1), pattern, mask, 1);
            line(builder, world, max.withX(min.x()), max.withX(min.x()).withY(min.y()), pattern, mask, 1);
            line(builder, world, max.withZ(min.z()), max.withZ(min.z()).withY(min.y()), pattern, mask, 1);
            line(builder, world, max.withY(min.y()), max.withY(min.y()).withX(min.x()), pattern, mask, 1);
            line(builder, world, max.withY(min.y()), max.withY(min.y()).withZ(min.z()), pattern, mask, 1);
            line(builder, world, min.withY(max.y()), min.withY(max.y()).withX(max.x()), pattern, mask, 1);
            line(builder, world, min.withY(max.y()), min.withY(max.y()).withZ(max.z()), pattern, mask, 1);

            return builder.build();
        };
    }

    public static @NotNull ComputeFunc floodFill(
            @NotNull Point center,
            @Range(from = 1, to = Integer.MAX_VALUE) int radius,
            @NotNull Pattern pattern
    ) {
        return floodFill(center, radius, pattern, Mask.always());
    }

    private static Set<Point> floodFill(
            @NotNull Point center,
            @Range(from = 1, to = Integer.MAX_VALUE) int radius,
            @NotNull Mask mask,
            @NotNull WorldView world
    ) {
        var queue = new LinkedList<Point>();

        queue.add(new BlockVec(center));
        var positions = new HashSet<>(queue);

        final Direction[] values = Direction.values();
        while (!queue.isEmpty()) {
            var current = queue.pop();
            for (Direction value : values) {
                final Point add = new BlockVec(current.add(value.vec()));
                if (!positions.contains(add) && add.distance(center) < radius && mask.test(world, add, world.getBlock(add))) {
                    queue.add(add);
                    positions.add(add);
                }
            }
        }

        return positions;
    }

    public static @NotNull ComputeFunc floodFill(
            @NotNull Point center,
            @Range(from = 1, to = Integer.MAX_VALUE) int radius,
            @NotNull Pattern pattern,
            @NotNull Mask mask
    ) {
        return (_, world) -> {
            var vec = new Vec(radius);
            var min = center.sub(vec);
            var max = center.add(vec);
            var builder = BlockBuffer.builder(world, min, max);

            for (Point position : floodFill(center, radius, mask, world)) {
                setBlock(builder, world, position, pattern);
            }

            return builder.build();
        };
    }

    public static @NotNull ComputeFunc drain(
            @NotNull Point center,
            @Range(from = 1, to = Integer.MAX_VALUE) int radius,
            @NotNull Mask mask
    ) {
        return (_, world) -> {
            var vec = new Vec(radius);
            var min = center.sub(vec);
            var max = center.add(vec);
            var builder = BlockBuffer.builder(world, min, max);

            for (Point position : floodFill(center, radius, mask, world)) {
                final Block block = world.getBlock(position);
                if (block.isLiquid()) {
                    builder.set(position, Block.AIR);
                } else if ("true".equals(block.getProperty("waterlogged"))) {
                    builder.set(position, block.withProperty("waterlogged", "false"));
                }
            }

            return builder.build();
        };
    }

    static void setBlock(@NotNull BlockBuffer.Builder target, @NotNull WorldView source, @NotNull Point pos, @NotNull Pattern pattern) throws InterruptedException {
        target.set(pos, pattern.blockAt(source, pos));
    }
}
