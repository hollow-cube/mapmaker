package net.hollowcube.terraform.compute;

import net.hollowcube.terraform.buffer.BlockBuffer;
import net.hollowcube.terraform.mask.Mask;
import net.hollowcube.terraform.pattern.Pattern;
import net.hollowcube.terraform.selection.region.CuboidRegion;
import net.hollowcube.terraform.selection.region.Region;
import net.hollowcube.terraform.task.ComputeFunc;
import net.hollowcube.terraform.task.edit.WorldView;
import net.hollowcube.terraform.util.math.DirectionUtil;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.Direction;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public final class RegionFunctions {

    public static @NotNull ComputeFunc replace(@NotNull Region region, @NotNull Mask mask, @NotNull Pattern pattern) {
        return (task, world) -> {
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
        var faceSet = faces.length > 0 ? EnumSet.copyOf(List.of(faces)) : EnumSet.allOf(Direction.class);

        return (task, world) -> {
            var buffer = BlockBuffer.builder(world, region.min(), region.max());

            for (int x = region.min().blockX(); x < region.max().blockX(); x++) {
                for (int y = region.min().blockY(); y < region.max().blockY(); y++) {
                    for (int z = region.min().blockZ(); z < region.max().blockZ(); z++) {

                        // If not hollow always set the block
                        var pos = new Vec(x, y, z);
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

        return (task, world) -> {
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
        return (task, world) -> {
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

    static void setBlock(@NotNull BlockBuffer.Builder target, @NotNull WorldView source, @NotNull Point pos, @NotNull Pattern pattern) throws InterruptedException {
        target.set(pos, pattern.blockAt(source, pos));
    }

    private RegionFunctions() {
    }

}
