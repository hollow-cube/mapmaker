package net.hollowcube.anticheat.protocol;

/// The lossy-packed vector 26.2 uses for entity velocity on the wire (`LpVec3`): a 48 bit buffer
/// holding three 15 bit signed fractions plus a 2 bit scale, and an optional varint carrying the
/// rest of the scale when the continuation bit is set.
///
/// The packed form is kept rather than three doubles because quantisation is not idempotent — a
/// decode/encode round trip through `Vec3` can pick a smaller scale and change the bytes.
public record LpVec3(long packed, int scaleHigh) {

    public static final LpVec3 ZERO = new LpVec3(0, 0);

    private static final int CONTINUATION_FLAG = 4;

    public static LpVec3 decode(ByteReader reader) {
        int lowest = reader.u8();
        if (lowest == 0) return ZERO;
        int middle = reader.u8();
        long highest = reader.i32() & 0xFFFFFFFFL;
        long packed = highest << 16 | (long) middle << 8 | lowest;
        int scaleHigh = (lowest & CONTINUATION_FLAG) == CONTINUATION_FLAG ? reader.varInt() : 0;
        return new LpVec3(packed, scaleHigh);
    }

    public void encode(ByteWriter writer) {
        if (packed == 0) {
            writer.u8(0);
            return;
        }
        writer.u8((int) packed);
        writer.u8((int) (packed >> 8));
        writer.i32((int) (packed >> 16));
        if ((packed & CONTINUATION_FLAG) == CONTINUATION_FLAG) writer.varInt(scaleHigh);
    }

    public double x() {
        return packed == 0 ? 0 : unpack(packed >> 3) * scale();
    }

    public double y() {
        return packed == 0 ? 0 : unpack(packed >> 18) * scale();
    }

    public double z() {
        return packed == 0 ? 0 : unpack(packed >> 33) * scale();
    }

    private long scale() {
        long scale = packed & 3;
        if ((packed & CONTINUATION_FLAG) == CONTINUATION_FLAG)
            scale |= (scaleHigh & 0xFFFFFFFFL) << 2;
        return scale;
    }

    private static double unpack(long value) {
        return Math.min(value & 32767L, 32766.0) * 2.0 / 32766.0 - 1.0;
    }
}
