package net.hollowcube.compat.axiom;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public final class Bytes {

    private Bytes() {
    }

    public static byte[] of(int... values) {
        var bytes = new byte[values.length];
        for (int i = 0; i < values.length; i++) bytes[i] = (byte) values[i];
        return bytes;
    }

    public static byte[] concat(byte[]... parts) {
        var out = new ByteArrayOutputStream();
        for (var part : parts) out.writeBytes(part);
        return out.toByteArray();
    }

    public static byte[] varInt(int value) {
        var out = new ByteArrayOutputStream();
        while ((value & ~0x7F) != 0) {
            out.write((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.write(value);
        return out.toByteArray();
    }

    public static byte[] string(String value) {
        var utf8 = value.getBytes(StandardCharsets.UTF_8);
        return concat(varInt(utf8.length), utf8);
    }

    public static byte[] int32(int value) {
        return of(value >>> 24, value >>> 16, value >>> 8, value);
    }

    public static byte[] int64(long value) {
        return concat(int32((int) (value >>> 32)), int32((int) value));
    }

    public static byte[] repeat(int length, int seed) {
        var bytes = new byte[length];
        for (int i = 0; i < length; i++) bytes[i] = (byte) ((i * 31 + seed) ^ (i >>> 3));
        return bytes;
    }
}
