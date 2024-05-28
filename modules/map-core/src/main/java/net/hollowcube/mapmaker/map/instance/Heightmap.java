package net.hollowcube.mapmaker.map.instance;

import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.MathUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static net.minestom.server.utils.chunk.ChunkUtils.toSectionRelativeCoordinate;

@SuppressWarnings("UnstableApiUsage")
public class Heightmap {
    private static final int DATA_SIZE = Chunk.CHUNK_SIZE_X * Chunk.CHUNK_SIZE_Z;

    public static final boolean SURFACE = true;
    public static final boolean BOTTOM = false;

    private final Chunk chunk;
    private final boolean type;
    private final int worldMin, worldMax;
    private final Predicate<Block> filter;

    // Note that in data, 0 is a missing value.
    // null == ungenerated, length 0 == no stored data, length DATA_SIZE == data
    private short[] data = null;

    public Heightmap(@NotNull Chunk chunk, boolean type, @NotNull Predicate<Block> filter) {
        this.chunk = chunk;
        this.type = type;
        var dimensionType = chunk.getInstance().getCachedDimensionType();
        this.worldMin = dimensionType.minY();
        this.worldMax = dimensionType.minY() + dimensionType.height();
        this.filter = filter;
    }

    public int get(int x, int z) {
        if (data == null || data.length == 0) return worldMin - 1;
        return data[toSectionRelativeCoordinate(z) << 4 | toSectionRelativeCoordinate(x)] + worldMin - 1;
    }

    public void update(int x, int y, int z, @NotNull Block block) {
        int current = get(x, z);
        if (current >= worldMin && (type ? y <= current - 2 : y >= current + 2)) {
            return;
        }

        if (filter.test(block)) {
            // Block was added
            if (current < worldMin || (type ? y >= current : y <= current)) {
                ensureData()[z << 4 | x] = (short) (y - worldMin + 1);
            }
        } else {
            // Block was removed, step from that block until we find a relevant one
            int offset = type ? -1 : 1;
            for (int i = y + offset; i >= worldMin && i < worldMax; i += offset) {
                if (filter.test(chunk.getBlock(x, i, z, Block.Getter.Condition.TYPE))) {
                    ensureData()[z << 4 | x] = (short) (i - worldMin + 1);
                    return;
                }
            }
            // There was no block, set to the bottom
            ensureData()[z << 4 | x] = (short) 0;
        }
    }

    public void load(int @Nullable [] data) {
        if (data == null) {
            recalculate();
            return;
        }
        if (data.length == 0) {
            this.data = new short[0];
            return;
        }

        this.data = new short[DATA_SIZE];
        for (int i = 0; i < data.length; i++) {
            this.data[i] = (short) data[i];
        }
    }

    public int[] save() {
        if (data == null) return null;
        if (data.length == 0) return new int[0];

        int[] result = new int[DATA_SIZE];
        for (int i = 0; i < data.length; i++) {
            result[i] = data[i];
        }
        return result;
    }

    public void recalculate() {
        this.data = new short[0]; // Empty, but present
        synchronized (chunk) {
            int startY = type ? worldMax - 1 : worldMin;

            // Try to skip any sections which are totally air because they are always irrelevant
            int sectionCount = chunk.getMaxSection() - chunk.getMinSection();
            for (int i = 0; i < sectionCount; i++) {
                int sectionY = type ? chunk.getMaxSection() - i - 1 : chunk.getMinSection() + i;
                var blockPalette = chunk.getSection(sectionY).blockPalette();
                if (blockPalette.count() != 0) break;
                startY += type ? -16 : 16;
            }

            for (int z = 0; z < Chunk.CHUNK_SIZE_Z; z++) {
                inner:
                for (int x = 0; x < Chunk.CHUNK_SIZE_X; x++) {
                    int y = startY;
                    while (y >= worldMin && y < worldMax) {
                        Block block = chunk.getBlock(x, y, z, Block.Getter.Condition.TYPE);
                        if (filter.test(block)) {
                            ensureData()[z << 4 | x] = (short) ((y - worldMin) + 1);
                            continue inner;
                        }
                        y += type ? -1 : 1;
                    }
                }
            }
        }
    }

    public long[] encode() {
        if (this.data == null) recalculate();
        short[] heightmap = this.data;
        if (heightmap.length == 0) return null; // No data, client will init to zero.
        return encodeEntries(ensureData(), MathUtils.bitsToRepresent(worldMax - worldMin));
    }

    private short[] ensureData() {
        if (data == null || data.length == 0) {
            data = new short[DATA_SIZE];
        }
        return data;
    }

    private static final int[] MAGIC = {
            -1, -1, 0, Integer.MIN_VALUE, 0, 0, 1431655765, 1431655765, 0, Integer.MIN_VALUE,
            0, 1, 858993459, 858993459, 0, 715827882, 715827882, 0, 613566756, 613566756,
            0, Integer.MIN_VALUE, 0, 2, 477218588, 477218588, 0, 429496729, 429496729, 0,
            390451572, 390451572, 0, 357913941, 357913941, 0, 330382099, 330382099, 0, 306783378,
            306783378, 0, 286331153, 286331153, 0, Integer.MIN_VALUE, 0, 3, 252645135, 252645135,
            0, 238609294, 238609294, 0, 226050910, 226050910, 0, 214748364, 214748364, 0,
            204522252, 204522252, 0, 195225786, 195225786, 0, 186737708, 186737708, 0, 178956970,
            178956970, 0, 171798691, 171798691, 0, 165191049, 165191049, 0, 159072862, 159072862,
            0, 153391689, 153391689, 0, 148102320, 148102320, 0, 143165576, 143165576, 0,
            138547332, 138547332, 0, Integer.MIN_VALUE, 0, 4, 130150524, 130150524, 0, 126322567,
            126322567, 0, 122713351, 122713351, 0, 119304647, 119304647, 0, 116080197, 116080197,
            0, 113025455, 113025455, 0, 110127366, 110127366, 0, 107374182, 107374182, 0,
            104755299, 104755299, 0, 102261126, 102261126, 0, 99882960, 99882960, 0, 97612893,
            97612893, 0, 95443717, 95443717, 0, 93368854, 93368854, 0, 91382282, 91382282,
            0, 89478485, 89478485, 0, 87652393, 87652393, 0, 85899345, 85899345, 0,
            84215045, 84215045, 0, 82595524, 82595524, 0, 81037118, 81037118, 0, 79536431,
            79536431, 0, 78090314, 78090314, 0, 76695844, 76695844, 0, 75350303, 75350303,
            0, 74051160, 74051160, 0, 72796055, 72796055, 0, 71582788, 71582788, 0,
            70409299, 70409299, 0, 69273666, 69273666, 0, 68174084, 68174084, 0, Integer.MIN_VALUE,
            0, 5};

    private static long[] encodeEntries(short[] heightmap, int bitsPerEntry) {
        final long maxEntryValue = (1L << bitsPerEntry) - 1;
        final char valuesPerLong = (char) (64 / bitsPerEntry);
        final int magicIndex = 3 * (valuesPerLong - 1);
        final long divideMul = Integer.toUnsignedLong(MAGIC[magicIndex]);
        final long divideAdd = Integer.toUnsignedLong(MAGIC[magicIndex + 1]);
        final int divideShift = MAGIC[magicIndex + 2];
        final int size = (heightmap.length + valuesPerLong - 1) / valuesPerLong;

        long[] data = new long[size];

        for (int i = 0; i < heightmap.length; i++) {
            final long value = heightmap[i];
            final int cellIndex = (int) (i * divideMul + divideAdd >> 32L >> divideShift);
            final int bitIndex = (i - cellIndex * valuesPerLong) * bitsPerEntry;
            data[cellIndex] = data[cellIndex] & ~(maxEntryValue << bitIndex) | (value & maxEntryValue) << bitIndex;
        }

        return data;
    }

}
