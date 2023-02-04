package net.hollowcube.terraform.mask;

import net.hollowcube.terraform.action.edit.WorldView;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

public interface Mask {

    // Constants

    static @NotNull Mask always() {
        return (world, point, block) -> true;
    }

    static @NotNull Mask never() {
        return (world, point, block) -> false;
    }

    // Logic operations

    static @NotNull Mask and(@NotNull Mask... masks) {
        return (world, point, block) -> {
            for (Mask mask : masks) {
                if (!mask.test(world, point, block)) {
                    return false;
                }
            }
            return true;
        };
    }

    static @NotNull Mask or(@NotNull Mask... masks) {
        return (world, point, block) -> {
            for (Mask mask : masks) {
                if (mask.test(world, point, block)) {
                    return true;
                }
            }
            return false;
        };
    }

    static @NotNull Mask not(@NotNull Mask mask) {
        return (world, point, block) -> !mask.test(world, point, block);
    }


    // Impl

    /**
     * Tests if this mask is allowed at the given point in the given {@link WorldView}.
     *
     * @return true to allow affecting the block, false to disallow
     */
    boolean test(@NotNull WorldView world, @NotNull Point point, @NotNull Block block);

}
