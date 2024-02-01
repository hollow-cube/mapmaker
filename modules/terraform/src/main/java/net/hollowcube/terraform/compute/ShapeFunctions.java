package net.hollowcube.terraform.compute;

import net.hollowcube.terraform.buffer.BlockBuffer;
import net.hollowcube.terraform.pattern.Pattern;
import net.hollowcube.terraform.task.ComputeFunc;
import net.hollowcube.terraform.util.math.CoordinateUtil;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

/**
 * Contains functions for computing a variety of common shapes.
 */
public final class ShapeFunctions {

    public static @NotNull ComputeFunc sphere(@NotNull Point position, @NotNull Pattern pattern, @NotNull Vec radius, boolean hollow) {
        Check.argCondition(radius.x() < 1 || radius.y() < 1 || radius.z() < 1, "radius must be >= 1");

        var ceilRadius = CoordinateUtil.ceil(radius.add(0.5));
        var inverseSq = Vec.ONE.div(radius.mul(radius));

        return (task, world) -> {
            var buffer = BlockBuffer.builder(world, position.sub(ceilRadius), position.add(ceilRadius));

            for (int x = 0; x <= ceilRadius.blockX(); x++) {
                var xSq = x * x * inverseSq.x();
                var nextXSq = (x + 1) * (x + 1) * inverseSq.x();

                y:
                for (int y = 0; y <= ceilRadius.blockY(); y++) {
                    var ySq = y * y * inverseSq.y();
                    var nextYSq = (y + 1) * (y + 1) * inverseSq.y();

                    for (int z = 0; z <= ceilRadius.blockZ(); z++) {
                        var zSq = z * z * inverseSq.z();

                        // If outside the sphere any remaining points are invalid so can continue immediately.
                        if (xSq + ySq + zSq >= 1.0 - 0.1) {
                            if (z == 0) {
                                if (y == 0) return buffer.build();
                                else break y;
                            } else break;
                        }

                        // If it is supposed to be hollow, we need to see if the next layer exists. If it does, skip this one.
                        if (hollow) {
                            var nextZSq = (z + 1) * (z + 1) * inverseSq.z();
                            if (nextXSq + ySq + zSq < 1.0 - 0.1 && xSq + nextYSq + zSq < 1.0 - 0.1 && xSq + ySq + nextZSq < 1.0 - 0.1)
                                continue;
                        }

                        // We compute one octant and mirror it across all axes
                        var pos = position.add(x, y, z);
                        buffer.set(pos, pattern.blockAt(world, pos));
                        pos = position.add(-x, y, z);
                        buffer.set(pos, pattern.blockAt(world, pos));
                        pos = position.add(x, -y, z);
                        buffer.set(pos, pattern.blockAt(world, pos));
                        pos = position.add(x, y, -z);
                        buffer.set(pos, pattern.blockAt(world, pos));
                        pos = position.add(-x, -y, z);
                        buffer.set(pos, pattern.blockAt(world, pos));
                        pos = position.add(x, -y, -z);
                        buffer.set(pos, pattern.blockAt(world, pos));
                        pos = position.add(-x, y, -z);
                        buffer.set(pos, pattern.blockAt(world, pos));
                        pos = position.add(-x, -y, -z);
                        buffer.set(pos, pattern.blockAt(world, pos));
                    }
                }
            }
            return buffer.build();
        };
    }

    /**
     * Creates a {@link ComputeFunc} that creates a cylinder shape at the given position with the given pattern and dimensions.
     *
     * <p>The dimensions are <x radius, height, y radius>.</p>
     *
     * @param position   The position to place the bottom center of the cylinder
     * @param pattern    The pattern to use for the blocks in the cylinder
     * @param dimensions The dimensions of the cylinder, see above
     * @param hollow     Whether the cylinder should be hollow or solid. Note that hollow cylinders are missing the end faces
     * @return a new compute func with the given settings
     */
    public static @NotNull ComputeFunc cylinder(@NotNull Point position, @NotNull Pattern pattern, @NotNull Vec dimensions, boolean hollow) {
        Check.argCondition(dimensions.x() < 1 || dimensions.y() < 1 || dimensions.z() < 1, "dimensions must be >= 1");

        var radiusX = dimensions.x() + 0.5;
        var radiusZ = dimensions.z() + 0.5;
        var inverseSqX = 1 / (radiusX * radiusX);
        var inverseSqZ = 1 / (radiusZ * radiusZ);

        return (task, world) -> {
            var buffer = BlockBuffer.builder(world,
                    position.sub(dimensions.x(), 0, dimensions.z()),
                    position.add(dimensions.x(), dimensions.y(), dimensions.z())
            );

            for (int x = 0; x <= (int) Math.ceil(radiusX); x++) {
                var xSq = x * x * inverseSqX;
                var nextXSq = (x + 1) * (x + 1) * inverseSqX;
                for (int z = 0; z <= (int) Math.ceil(radiusZ); z++) {
                    var zSq = z * z * inverseSqZ;

                    // If outside the circle continue to the next line
                    if (xSq + zSq > 1) {
                        if (z == 0) return buffer.build();
                        else break;
                    }

                    // If it is supposed to be hollow, we need to see if the next layer exists. If it does, skip this one.
                    if (hollow && nextXSq + zSq <= 1 && xSq + (z + 1) * (z + 1) * inverseSqZ <= 1) {
                        continue;
                    }

                    // Fill in all 4 quadrants for each height
                    for (int y = 0; y < dimensions.blockY(); ++y) {
                        var pos = position.add(x, y, z);
                        buffer.set(pos, pattern.blockAt(world, pos));
                        pos = position.add(-x, y, z);
                        buffer.set(pos, pattern.blockAt(world, pos));
                        pos = position.add(x, y, -z);
                        buffer.set(pos, pattern.blockAt(world, pos));
                        pos = position.add(-x, y, -z);
                        buffer.set(pos, pattern.blockAt(world, pos));
                    }
                }
            }

            return buffer.build();
        };
    }

    /**
     * Creates a {@link ComputeFunc} that creates a pyramid shape at the given position with the given pattern and height.
     *
     * @param position The position of the pyramid (the bottom center)
     * @param pattern  The pattern to use for the blocks in the pyramid
     * @param height   The height of the pyramid in blocks. This is the number of levels to include
     * @param hollow   Whether the pyramid should be hollow or solid. Note that hollow pyramids are missing the bottom face
     * @return a new compute func with the given settings
     */
    public static @NotNull ComputeFunc pyramid(@NotNull Point position, @NotNull Pattern pattern, int height, boolean hollow) {
        Check.argCondition(height < 1, "height must be >= 1");

        return (task, world) -> {
            //noinspection SuspiciousNameCombination
            var buffer = BlockBuffer.builder(world,
                    position.sub(height, 0, height),
                    position.add(height, height, height)
            );

            for (int i = 0; i < height; i++) {
                var radius = height - i - 1;
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        if (hollow && x != -radius && x != radius && z != -radius && z != radius)
                            continue;

                        var pos = position.add(x, i, z);
                        buffer.set(pos, pattern.blockAt(world, pos));
                    }
                }
            }

            return buffer.build();
        };
    }

    private ShapeFunctions() {
    }
}
