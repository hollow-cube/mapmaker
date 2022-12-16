package net.hollowcube.terraform.region;

import net.hollowcube.terraform.util.CoordinateUtil;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public record CuboidRegion(
        @NotNull Instance instance,
        @NotNull Point pos1,
        @NotNull Point pos2
) implements Region {

    @Override
    public @NotNull Point min() {
        return CoordinateUtil.min(pos1, pos2);
    }

    @Override
    public @NotNull Point max() {
        return CoordinateUtil.max(pos1, pos2);
    }

    @NotNull
    @Override
    public Iterator<@NotNull Point> iterator() {
        return new RegionIterator();
    }

    private class RegionIterator implements Iterator<@NotNull Point> {
        private final int maxX, maxY, maxZ;
        private int x, y, z;

        public RegionIterator() {
            Point max = max(), min = min();
            maxX = max.blockX();
            maxY = max.blockY();
            maxZ = max.blockZ();
            x = min.blockX();
            y = min.blockY();
            z = min.blockZ();
        }

        @Override
        public boolean hasNext() {
            return x <= maxX && y <= maxY && z <= maxZ;
        }

        @Override
        public @NotNull Point next() {
            // Iterate over cuboid
            Point point = new Vec(x, y, z);
            if (++x > maxX) {
                x = min().blockX();
                if (++y > maxY) {
                    y = min().blockY();
                    z++;
                }
            }
            return point;
        }
    }
}
