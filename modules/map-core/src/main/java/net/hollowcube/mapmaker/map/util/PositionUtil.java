package net.hollowcube.mapmaker.map.util;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.NotNull;

public final class PositionUtil {

    public static long packPosition(@NotNull Point position) {
        return packPosition(position.blockX(), position.blockY(), position.blockZ());
    }

    public static long packPosition(int x, int y, int z) {
        return ((long) (x & 0x1FFFFFF) << 38) | ((long) (z & 0x1FFFFFF) << 13) | (long) (y & 0x1FFF);
    }

    public static @NotNull Point unpackPosition(long packedPosition) {
        int x = (int) (packedPosition >> 38) & 0x1FFFFFF;
        int z = (int) (packedPosition >> 13) & 0x1FFFFFF;
        int y = (int) packedPosition & 0x1FFF;

        if (x >= 1 << 24) x -= 1 << 25;  // 25-bit signed range: -16777216 to 16777215
        if (z >= 1 << 24) z -= 1 << 25;  // 25-bit signed range: -16777216 to 16777215
        if (y >= 1 << 12) y -= 1 << 13;  // 13-bit signed range: -4096 to 4095

        return new Vec(x, y, z);
    }

}
