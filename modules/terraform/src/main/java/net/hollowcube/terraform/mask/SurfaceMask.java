package net.hollowcube.terraform.mask;

import net.hollowcube.terraform.action.edit.WorldView;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.Direction;
import org.jetbrains.annotations.NotNull;

/**
 * Matches any <b>solid, non-air</b> block which is exposed to the surface on at least one face.
 */
public class SurfaceMask implements Mask {
    @Override
    public boolean test(@NotNull WorldView world, @NotNull Point point, @NotNull Block block) {
        if (block.isAir() || !block.isSolid())
            return false;

        for (var direction : Direction.values()) {
            var neighbor = world.getBlock(point.add(direction.normalX(), direction.normalY(), direction.normalZ()));
            if (neighbor.isAir())
                return true;
        }

        return false;
    }
}
