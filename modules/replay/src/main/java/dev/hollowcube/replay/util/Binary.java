package dev.hollowcube.replay.util;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

import static net.minestom.server.network.NetworkBuffer.SHORT;
import static net.minestom.server.network.NetworkBuffer.VAR_INT;

@SuppressWarnings("UnstableApiUsage")
public final class Binary {
    private Binary() {
    }

    public static void writeRelativePos(@NotNull NetworkBuffer buffer, @NotNull Pos pos) {
        writePosComponent(buffer, pos.x());
        writePosComponent(buffer, pos.y());
        writePosComponent(buffer, pos.z());
        buffer.write(SHORT, (short) toHalfFloat(pos.yaw()));
        buffer.write(SHORT, (short) toHalfFloat(pos.pitch()));
    }

    public static @NotNull Pos readRelativePos(@NotNull NetworkBuffer buffer) {
        var x = readPosComponent(buffer);
        var y = readPosComponent(buffer);
        var z = readPosComponent(buffer);

        var yaw = 0f;
        var pitch = 0f;

        return new Pos(x, y, z, yaw, pitch);
    }

    public static void writePosComponent(@NotNull NetworkBuffer buffer, double value) {
        buffer.write(VAR_INT, Math.abs((int) value));
        buffer.write(SHORT, (short) toHalfFloat((float) (value - (int) value)));
    }

    public static double readPosComponent(@NotNull NetworkBuffer buffer) {
        double value = buffer.read(VAR_INT);
        value += fromHalfFloat(buffer.read(SHORT));
        return value;
    }

    // https://stackoverflow.com/a/6162687
    public static int toHalfFloat(float fval) {
        int fbits = Float.floatToIntBits(fval);
        int sign = fbits >>> 16 & 0x8000;
        int val = (fbits & 0x7fffffff) + 0x1000;

        if (val >= 0x47800000) {
            if ((fbits & 0x7fffffff) >= 0x47800000) {
                if (val < 0x7f800000)
                    return sign | 0x7c00;
                return sign | 0x7c00 | (fbits & 0x007fffff) >>> 13;
            }
            return sign | 0x7bff;
        }
        if (val >= 0x38800000)
            return sign | val - 0x38000000 >>> 13;
        if (val < 0x33000000)
            return sign;
        val = (fbits & 0x7fffffff) >>> 23;
        return sign | ((fbits & 0x7fffff | 0x800000) + (0x800000 >>> val - 102) >>> 126 - val);
    }

    // https://stackoverflow.com/a/6162687
    public static float fromHalfFloat(int hbits) {
        int mant = hbits & 0x03ff;
        int exp = hbits & 0x7c00;
        if (exp == 0x7c00) {
            exp = 0x3fc00;
        } else if (exp != 0) {
            exp += 0x1c000;
            if (mant == 0 && exp > 0x1c400) {
                return Float.intBitsToFloat((hbits & 0x8000) << 16 | exp << 13 | 0x3ff);
            }
        } else if (mant != 0) {
            exp = 0x1c400;
            do {
                mant <<= 1;
                exp -= 0x400;
            } while ((mant & 0x400) == 0);
            mant &= 0x3ff;
        }
        return Float.intBitsToFloat((hbits & 0x8000) << 16 | (exp | mant) << 13);
    }
}
