package net.hollowcube.mapmaker.map.util;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.NotNull;

public final class PositionUtil {

    public static long packPosition(@NotNull Point position) {
        return packPosition(position.blockX(), position.blockY(), position.blockZ());
    }

    public static long packPosition(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (long) (y & 0xFFF);
    }

    public static @NotNull Point unpackPosition(long packedPosition) {
        int x = (int) (packedPosition >> 38) & 0x3FFFFFF;
        int z = (int) (packedPosition >> 12) & 0x3FFFFFF;
        int y = (int) packedPosition & 0xFFF;
        // Adjust the sign bit if necessary for x and z
        if (x >= 1 << 25) x -= 1 << 26;
        if (z >= 1 << 25) z -= 1 << 26;
        return new Vec(x, y, z);
    }

}
