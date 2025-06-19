package net.hollowcube.terraform.util;

import net.hollowcube.common.util.BlockUtil;
import net.minestom.server.utils.validate.Check;

public final class PaletteUtil {
    private PaletteUtil() {
    }

    public static final int BLOCK_PALETTE_SIZE = 4096;

    public static final int MAX_BITS_PER_ENTRY;

    static {
        int bpe = 0;
        for (short id = 24134; id < Short.MAX_VALUE; id++) {
            if (BlockUtil.fromStateIdOrNull(id) == null) {
                bpe = (int) Math.ceil(Math.log(id) / Math.log(2));
                break;
            }
        }

        Check.stateCondition(bpe == 0, "Could not find max bits per entry");
        MAX_BITS_PER_ENTRY = bpe;
    }

    public static long packPos(int x, int y, int z) {
        return (((long) x & 67108863L) << 38) | ((long) y & 4095L) | (((long) z & 67108863L) << 12);
    }

    public static int unpackX(long packedPos) {
        return (int) (packedPos >> 38);
    }

    public static int unpackY(long packedPos) {
        return (int) ((packedPos << 52) >> 52);
    }

    public static int unpackZ(long packedPos) {
        return (int) ((packedPos << 26) >> 38);
    }

    public static long[] pack(int[] ints, int bitsPerEntry) {
        int intsPerLong = (int) Math.floor(64d / bitsPerEntry);
        long[] longs = new long[(int) Math.ceil(ints.length / (double) intsPerLong)];

        long mask = (1L << bitsPerEntry) - 1L;
        for (int i = 0; i < longs.length; i++) {
            for (int intIndex = 0; intIndex < intsPerLong; intIndex++) {
                int bitIndex = intIndex * bitsPerEntry;
                int intActualIndex = intIndex + i * intsPerLong;
                if (intActualIndex < ints.length) {
                    longs[i] |= (ints[intActualIndex] & mask) << bitIndex;
                }
            }
        }

        return longs;
    }

    public static void unpack(int[] out, long[] in, int bitsPerEntry) {
        assert in.length != 0 : "unpack input array is zero";

        var intsPerLong = Math.floor(64d / bitsPerEntry);
        var intsPerLongCeil = (int) Math.ceil(intsPerLong);

        long mask = (1L << bitsPerEntry) - 1L;
        for (int i = 0; i < out.length; i++) {
            int longIndex = i / intsPerLongCeil;
            int subIndex = i % intsPerLongCeil;

            out[i] = (int) ((in[longIndex] >>> (bitsPerEntry * subIndex)) & mask);
        }
    }
}
