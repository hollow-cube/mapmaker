package net.hollowcube.anticheat.protocol;

/// Packing of the three coordinate longs the vanilla wire format uses.
///
/// Layouts are taken from the 26.2 decompile: `BlockPos#asLong` (x 26 bits at 38, z 26 at 12,
/// y 12 at 0), `SectionPos#asLong` (x 22 at 42, z 22 at 20, y 20 at 0) and `ChunkPos#pack`
/// (x in the low int, z in the high int).
public final class Positions {

    public static final int BLOCK_X_BITS = 26;
    public static final int BLOCK_Y_BITS = 12;
    private static final int BLOCK_X_OFFSET = 38;
    private static final int BLOCK_Z_OFFSET = 12;

    public static long blockPos(int x, int y, int z) {
        return (x & 0x3FFFFFFL) << BLOCK_X_OFFSET | (z & 0x3FFFFFFL) << BLOCK_Z_OFFSET | (y & 0xFFFL);
    }

    public static int blockX(long packed) {
        return (int) (packed << 64 - BLOCK_X_OFFSET - BLOCK_X_BITS >> 64 - BLOCK_X_BITS);
    }

    public static int blockY(long packed) {
        return (int) (packed << 64 - BLOCK_Y_BITS >> 64 - BLOCK_Y_BITS);
    }

    public static int blockZ(long packed) {
        return (int) (packed << 64 - BLOCK_Z_OFFSET - BLOCK_X_BITS >> 64 - BLOCK_X_BITS);
    }

    public static long sectionPos(int x, int y, int z) {
        return (x & 0x3FFFFFL) << 42 | (z & 0x3FFFFFL) << 20 | (y & 0xFFFFFL);
    }

    public static int sectionX(long packed) {
        return (int) (packed >> 42);
    }

    public static int sectionY(long packed) {
        return (int) (packed << 44 >> 44);
    }

    public static int sectionZ(long packed) {
        return (int) (packed << 22 >> 42);
    }

    public static long chunkPos(int x, int z) {
        return x & 0xFFFFFFFFL | (z & 0xFFFFFFFFL) << 32;
    }

    public static int chunkX(long packed) {
        return (int) packed;
    }

    public static int chunkZ(long packed) {
        return (int) (packed >> 32);
    }

    private Positions() {}
}
