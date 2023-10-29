package net.hollowcube.terraform.mask;

import net.hollowcube.terraform.task.edit.WorldView;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

public class OffsetMask implements Mask {

    public static @NotNull OffsetMask overlay(@NotNull Mask mask) {
        return new OffsetMask(mask, 0, -1, 0);
    }

    public static @NotNull OffsetMask underlay(@NotNull Mask mask) {
        return new OffsetMask(mask, 0, 1, 0);
    }


    private final Mask mask;
    private final int x;
    private final int y;
    private final int z;

    protected OffsetMask(Mask mask, int x, int y, int z) {
        this.mask = mask;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean test(@NotNull WorldView world, @NotNull Point point, @NotNull Block block) {
        var newPoint = point.add(x, y, z);
        return mask.test(world, newPoint, world.getBlock(newPoint));
    }
}
