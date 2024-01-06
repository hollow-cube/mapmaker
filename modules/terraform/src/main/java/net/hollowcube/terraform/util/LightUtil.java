package net.hollowcube.terraform.util;

import net.minestom.server.instance.light.Light;
import org.jetbrains.annotations.NotNull;

public final class LightUtil {

    public static void setLevel(@NotNull Light light, int x, int y, int z, byte value) {
        var array = light.array();
        if (array == null || array.length == 0) {
            array = new byte[2048];
            light.set(array);
        }

        int index = x | (z << 4) | (y << 8);
        int shift = (index & 1) << 2;
        int i = index >>> 1;
        array[i] = (byte) ((array[i] & (0xF0 >>> shift)) | (value << shift));
    }
}
