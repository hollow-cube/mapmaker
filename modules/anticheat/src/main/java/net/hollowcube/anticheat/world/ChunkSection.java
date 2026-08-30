package net.hollowcube.anticheat.world;

import net.hollowcube.anticheat.protocol.Section;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/// The mutable half of a [Section]: the same paletted storage, writable a block at a time.
///
/// Sharing is owned by [Chunk], which knows whether it may write into a section in place or has to
/// [#copy()] it first, so this class never needs a flag of its own. The biome bytes are never
/// written and are always shared.
///
/// `nonEmptyBlockCount` and `fluidCount` are kept exactly as they arrived and are **not** recounted
/// on a write: the proxy has no block registry and so cannot tell which state ids are air. Nothing
/// in phase 0 reads them, and a reader that materialises the section reads block states out of the
/// palette.
final class ChunkSection {

    private final int nonEmptyBlockCount;
    private final int fluidCount;
    private final byte[] biomes;
    private int bitsPerEntry;
    private int @Nullable [] palette;
    private long[] data;

    private ChunkSection(int nonEmptyBlockCount, int fluidCount, byte[] biomes, int bitsPerEntry, int @Nullable [] palette, long[] data) {
        this.nonEmptyBlockCount = nonEmptyBlockCount;
        this.fluidCount = fluidCount;
        this.biomes = biomes;
        this.bitsPerEntry = bitsPerEntry;
        this.palette = palette;
        this.data = data;
    }

    static ChunkSection of(Section section) {
        return new ChunkSection(section.nonEmptyBlockCount(), section.fluidCount(), section.biomes(),
            section.bitsPerEntry(), section.palette(), section.data());
    }

    /// A section that may be written without disturbing this one. The arrays are the only mutable
    /// state, so cloning them is the whole copy.
    ChunkSection copy() {
        return new ChunkSection(nonEmptyBlockCount, fluidCount, biomes,
            bitsPerEntry, palette == null ? null : palette.clone(), data.clone());
    }

    /// The wire form again. The returned record shares this section's arrays, so the caller must
    /// already hold it through a snapshot (a live write clones before touching them).
    Section toSection() {
        return new Section(nonEmptyBlockCount, fluidCount, bitsPerEntry, palette, data, biomes);
    }

    long[] data() {
        return data;
    }

    int get(int x, int y, int z) {
        if (bitsPerEntry == 0) return palette == null ? 0 : palette[0];
        int value = read(data, Section.blockStorageBits(bitsPerEntry), index(x, y, z));
        return palette == null ? value : palette[value];
    }

    /// Writes one block state, growing the palette the way `PalettedContainer` does: a single-value
    /// section becomes a two-entry indirect one, an indirect palette with room left gains an entry,
    /// and a full one is repacked to the global palette rather than widened step by step — the
    /// widening path only saves bytes in a section that is being rewritten wholesale anyway.
    void set(int x, int y, int z, int stateId) {
        int index = index(x, y, z);
        if (bitsPerEntry == 0) {
            int current = palette == null ? 0 : palette[0];
            if (current == stateId) return;
            bitsPerEntry = 4;
            palette = new int[]{current, stateId};
            data = new long[Section.longCount(4, Section.BLOCK_ENTRY_COUNT)];
            write(data, 4, index, 1);
            return;
        }

        var palette = this.palette;
        if (palette == null) {
            write(data, Section.DIRECT_BLOCK_BITS, index, stateId);
            return;
        }

        int storageBits = Section.blockStorageBits(bitsPerEntry);
        for (int i = 0; i < palette.length; i++) {
            if (palette[i] != stateId) continue;
            write(data, storageBits, index, i);
            return;
        }

        if (palette.length < 1 << storageBits) {
            var grown = Arrays.copyOf(palette, palette.length + 1);
            grown[palette.length] = stateId;
            this.palette = grown;
            // 1..4 bits all store at four bits, so declaring the real width changes no packed byte.
            bitsPerEntry = Math.max(bitsPerEntry, storageBits);
            write(data, storageBits, index, palette.length);
            return;
        }

        toGlobalPalette(palette, storageBits);
        write(data, Section.DIRECT_BLOCK_BITS, index, stateId);
    }

    private void toGlobalPalette(int[] previous, int storageBits) {
        var direct = new long[Section.longCount(Section.DIRECT_BLOCK_BITS, Section.BLOCK_ENTRY_COUNT)];
        for (int i = 0; i < Section.BLOCK_ENTRY_COUNT; i++)
            write(direct, Section.DIRECT_BLOCK_BITS, i, previous[read(data, storageBits, i)]);
        bitsPerEntry = Section.DIRECT_BLOCK_BITS;
        palette = null;
        data = direct;
    }

    /// `Strategy#getIndex`: `(y << 4 | z) << 4 | x`.
    private static int index(int x, int y, int z) {
        return (y << 4 | z) << 4 | x;
    }

    private static int read(long[] data, int bits, int index) {
        int valuesPerLong = 64 / bits;
        int cell = index / valuesPerLong;
        int bitIndex = (index - cell * valuesPerLong) * bits;
        return (int) (data[cell] >>> bitIndex & (1L << bits) - 1);
    }

    private static void write(long[] data, int bits, int index, int value) {
        int valuesPerLong = 64 / bits;
        int cell = index / valuesPerLong;
        int bitIndex = (index - cell * valuesPerLong) * bits;
        long mask = (1L << bits) - 1;
        data[cell] = data[cell] & ~(mask << bitIndex) | (value & mask) << bitIndex;
    }
}
