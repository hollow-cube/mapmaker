package net.hollowcube.mapmaker.map.util;

import net.minestom.server.coordinate.Point;
import org.jetbrains.annotations.NotNull;

public final class PositionUtil {

    public static long packPosition(@NotNull Point position) {
        return packPosition(position.blockX(), position.blockY(), position.blockZ());
    }

    public static long packPosition(int x, int y, int z) {
        return ((long) x & 0x3FFFFFF) << 38 | ((long) z & 0x3FFFFFF) << 12 | (long) y & 0xFFF;
    }

}
