package net.hollowcube.anticheat.protocol;

import org.jetbrains.annotations.Nullable;

/// One 16x16x16 chunk section as it appears inside `ClientboundLevelChunkPacketData`'s buffer:
/// `short nonEmptyBlockCount`, `short fluidCount`, a paletted container of block states and a
/// paletted container of biomes (`LevelChunkSection#read`/`#write` in the 26.2 decompile).
///
/// Block states are decoded into global (client-pvn) state ids. Biomes are kept as the exact bytes
/// they arrived as: nothing above this cares about them, and keeping them verbatim is what makes
/// [#encode] byte-identical to the frame it was decoded from.
///
/// The palette shape follows `Strategy#createForBlockStates`: 0 bits is a single-value palette,
/// 1..8 an indirect list (with 1..4 stored at 4 bits in memory, exactly as the client does) and
/// anything above 8 the global palette at [#DIRECT_BLOCK_BITS].
public record Section(
    int nonEmptyBlockCount,
    int fluidCount,
    int bitsPerEntry,
    int @Nullable [] palette,
    long[] data,
    byte[] biomes
) {

    public static final int BLOCK_ENTRY_COUNT = 4096;
    public static final int BIOME_ENTRY_COUNT = 64;

    /// `Strategy#globalPaletteBitsInMemory` for the 26.2 block state registry: `ceillog2` of a
    /// registry with between 2^14 and 2^15 states. A direct-palette section is stored at this
    /// width no matter what the declared bits byte says, because that is what the client does.
    public static final int DIRECT_BLOCK_BITS = 15;

    public static Section decode(ByteReader reader) {
        int nonEmptyBlockCount = reader.i16();
        int fluidCount = reader.i16();

        int bitsPerEntry = reader.u8();
        int[] palette;
        if (bitsPerEntry == 0) {
            palette = new int[]{reader.varInt()};
        } else if (bitsPerEntry <= 8) {
            palette = reader.varIntArray();
        } else {
            palette = null;
        }
        long[] data = reader.fixedLongArray(longCount(blockStorageBits(bitsPerEntry), BLOCK_ENTRY_COUNT));

        int biomeStart = reader.index();
        skipBiomes(reader);
        return new Section(nonEmptyBlockCount, fluidCount, bitsPerEntry, palette, data, reader.since(biomeStart));
    }

    public void encode(ByteWriter writer) {
        writer.i16(nonEmptyBlockCount);
        writer.i16(fluidCount);
        writer.u8(bitsPerEntry);
        var palette = this.palette;
        if (bitsPerEntry == 0) {
            writer.varInt(palette == null ? 0 : palette[0]);
        } else if (bitsPerEntry <= 8) {
            writer.varIntArray(palette == null ? new int[0] : palette);
        }
        writer.fixedLongArray(data);
        writer.bytes(biomes);
    }

    /// The global block state id at a section-relative position, indexed the way
    /// `Strategy#getIndex` does: `(y << 4 | z) << 4 | x`.
    public int get(int x, int y, int z) {
        var palette = this.palette;
        if (bitsPerEntry == 0) return palette == null ? 0 : palette[0];

        int bits = blockStorageBits(bitsPerEntry);
        int index = (y << 4 | z) << 4 | x;
        int valuesPerLong = 64 / bits;
        int cell = index / valuesPerLong;
        int bitIndex = (index - cell * valuesPerLong) * bits;
        int id = (int) (data[cell] >>> bitIndex & (1L << bits) - 1);
        return palette == null ? id : palette[id];
    }

    public static int blockStorageBits(int bitsPerEntry) {
        if (bitsPerEntry == 0) return 0;
        if (bitsPerEntry <= 4) return 4;
        return bitsPerEntry <= 8 ? bitsPerEntry : DIRECT_BLOCK_BITS;
    }

    public static int longCount(int storageBits, int entryCount) {
        if (storageBits == 0) return 0;
        int valuesPerLong = 64 / storageBits;
        return (entryCount + valuesPerLong - 1) / valuesPerLong;
    }

    private static void skipBiomes(ByteReader reader) {
        int bits = reader.u8();
        if (bits == 0) {
            reader.varInt();
        } else if (bits <= 3) {
            int size = reader.varInt();
            for (int i = 0; i < size; i++) reader.varInt();
        }
        reader.skip(longCount(bits, BIOME_ENTRY_COUNT) * 8);
    }
}
