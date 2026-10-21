package net.hollowcube.terraform.buffer;

import it.unimi.dsi.fastutil.ints.*;
import net.hollowcube.terraform.util.HeapSize;
import net.hollowcube.terraform.util.PosConsumer;
import net.hollowcube.terraform.util.PosIntConsumer;
import net.hollowcube.terraform.util.PosPredicate;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.palette.Palette;
import net.minestom.server.instance.palette.PaletteHack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static net.minestom.server.coordinate.CoordConversion.*;

/// One section edit of blocks (& block entities/with data) and biomes. The three channels are all optional and lazy.
/// - **blocks**: adaptive palette encoded as 0 = [#LEAVE], stateId+1 = a block state.
/// - **block entities/with data**: simple map section index -> block
/// - **biomes**: 4x4x4 palette encoded as 0 = [#LEAVE], stateId+1 = a biome id.
///
/// Not thread-safe for writes.
public final class BufferSection {
    private static final int BLOCK_DIM = 16;
    private static final int BLOCK_CELLS = BLOCK_DIM * BLOCK_DIM * BLOCK_DIM; // 4096
    private static final int BLOCK_MIN_BITS = 1;
    private static final int BLOCK_MAX_BITS = 8;
    private static final int BLOCK_DIRECT_BITS = 16;
    private static final int BLOCK_SPARSE_LIMIT = 256;

    private static final int BIOME_DIM = 4;
    private static final int BIOME_MIN_BITS = 1;
    private static final int BIOME_MAX_BITS = 3;
    private static final int BIOME_DIRECT_BITS = 15;

    /// Internal palette value for [#NONE]. Converted when reading back (ie -1).
    private static final int LEAVE = 0;
    /// ID for "not set" aka untouched.
    public static final int NONE = -1;

    // Blocks: exactly one of these is non-null (sparse until dense).
    private @Nullable Int2ShortMap blockSparse;
    private @Nullable Palette blockPalette;

    private @Nullable Palette biomes;
    private @Nullable Int2ObjectMap<Block> blockEntities;
    private long blockEntityBytes; // Running sum of blockEntity bytes

    static BufferSection uniform(int stateId) {
        Palette palette = Palette.empty(
            BLOCK_DIM,
            BLOCK_MIN_BITS,
            BLOCK_MAX_BITS,
            BLOCK_DIRECT_BITS
        );
        palette.fill(stateId + 1); // offset-encoded (0 = leave)
        return new BufferSection(palette);
    }

    BufferSection() {
        var sparse = new Int2ShortOpenHashMap();
        sparse.defaultReturnValue((short) LEAVE); // absent cell reads as "leave"
        this.blockSparse = sparse;
    }

    BufferSection(Palette blocks) { // dense
        this.blockPalette = blocks;
    }

    // Blocks

    public int count() {
        return blockPalette != null ? blockPalette.count() : sparse().size();
    }

    public boolean hasBlocks() {
        return count() > 0;
    }

    public boolean hasBlockEntities() {
        return blockEntities != null && !blockEntities.isEmpty();
    }

    public int getBlockState(int x, int y, int z) {
        int value = blockPalette != null
            ? blockPalette.get(x, y, z)
            : sparse().get(sectionBlockIndex(x, y, z)) & 0xFFFF;
        return value == LEAVE ? NONE : value - 1;
    }

    // Returns a block entity only, or null even if there is a raw state set.
    public @Nullable Block getBlockEntity(int x, int y, int z) {
        return blockEntities == null ? null : blockEntities.get(sectionBlockIndex(x, y, z));
    }

    /// Returns the offset-encoded value if this section is full-cover and uniform, else [#NONE].
    public int uniformBlockValue() {
        if (blockPalette == null) return NONE;
        if (blockPalette.count() != BLOCK_CELLS) return NONE;
        int first = blockPalette.get(0, 0, 0);
        return blockPalette.count(first) == BLOCK_CELLS ? first : NONE;
    }

    void setBlockState(int x, int y, int z, int stateId) {
        if (blockPalette != null) {
            blockPalette.set(x, y, z, stateId + 1);
            return;
        }

        var sparse = sparse();
        sparse.put(sectionBlockIndex(x, y, z), (short) (stateId + 1));
        if (sparse.size() > BLOCK_SPARSE_LIMIT) densify();
    }

    void setBlockEntity(int x, int y, int z, Block block) {
        if (blockEntities == null) blockEntities = new Int2ObjectOpenHashMap<>();
        Block previous = blockEntities.put(sectionBlockIndex(x, y, z), block);
        //noinspection ConstantValue intellij dumb
        if (previous != null) blockEntityBytes -= HeapSize.block(previous);
        blockEntityBytes += HeapSize.block(block);
    }

    void removeBlockEntity(int x, int y, int z) {
        if (blockEntities == null) return;
        Block previous = blockEntities.remove(sectionBlockIndex(x, y, z));
        //noinspection ConstantValue intellij dumb
        if (previous != null) blockEntityBytes -= HeapSize.block(previous);
    }

    /// Runs for every present block (ie [#LEAVE] ignored), coordinates are section-local.
    public void forEachBlockState(PosIntConsumer consumer) {
        if (blockPalette != null) {
            blockPalette.getAllPresent((x, y, z, value) -> consumer.accept(x, y, z, value - 1));
            return;
        }

        for (Int2ShortMap.Entry entry : Int2ShortMaps.fastIterable(sparse())) {
            int index = entry.getIntKey();
            consumer.accept(
                sectionBlockIndexGetX(index),
                sectionBlockIndexGetY(index),
                sectionBlockIndexGetZ(index),
                (entry.getShortValue() & 0xFFFF) - 1
            );
        }
    }

    /// Runs for every present block entity (ie raw state or [#LEAVE] ignored), coordinates are section-local.
    public void forEachBlockEntity(PosConsumer<Block> consumer) {
        if (blockEntities == null) return;
        for (Int2ObjectMap.Entry<Block> entry : Int2ObjectMaps.fastIterable(blockEntities)) {
            int index = entry.getIntKey();
            consumer.accept(
                sectionBlockIndexGetX(index),
                sectionBlockIndexGetY(index),
                sectionBlockIndexGetZ(index),
                entry.getValue()
            );
        }
    }

    /// Drops every set block cell (and its block-entity data) for which `keep` returns false, at
    /// section-relative block coordinates (0..15). Returns [#hasBlocks()] afterwards.
    boolean filterBlocks(PosPredicate keep) {
        IntArrayList drop = new IntArrayList();
        if (blockPalette != null) {
            blockPalette.getAllPresent((x, y, z, _) -> {
                if (!keep.test(x, y, z)) drop.add(sectionBlockIndex(x, y, z));
            });
            for (int i = 0; i < drop.size(); i++) {
                int index = drop.getInt(i);
                blockPalette.set(
                    sectionBlockIndexGetX(index),
                    sectionBlockIndexGetY(index),
                    sectionBlockIndexGetZ(index),
                    LEAVE
                );
            }
        } else {
            var sparse = sparse();
            for (Int2ShortMap.Entry entry : Int2ShortMaps.fastIterable(sparse)) {
                int index = entry.getIntKey();
                boolean kept = keep.test(
                    sectionBlockIndexGetX(index),
                    sectionBlockIndexGetY(index),
                    sectionBlockIndexGetZ(index)
                );
                if (!kept) drop.add(index);
            }
            for (int i = 0; i < drop.size(); i++) sparse.remove(drop.getInt(i));
        }

        if (blockEntities != null) {
            for (int i = 0; i < drop.size(); i++) {
                int index = drop.getInt(i);
                removeBlockEntity(
                    sectionBlockIndexGetX(index),
                    sectionBlockIndexGetY(index),
                    sectionBlockIndexGetZ(index)
                );
            }
        }

        return hasBlocks();
    }

    // Biomes

    public boolean hasBiomes() {
        return biomes != null && biomes.count() > 0;
    }

    public int getBiome(int x, int y, int z) {
        if (biomes == null) return NONE;
        int value = biomes.get(x, y, z);
        return value == LEAVE ? NONE : value - 1;
    }

    void setBiome(int x, int y, int z, int biomeId) {
        if (biomes == null) {
            biomes = Palette.empty(BIOME_DIM, BIOME_MIN_BITS, BIOME_MAX_BITS, BIOME_DIRECT_BITS);
        }
        biomes.set(x, y, z, biomeId + 1);
    }

    /// Runs for every present biome (ie [#LEAVE] ignored), coordinates are section-local biome coordinates (0 to 3).
    public void forEachBiome(PosIntConsumer consumer) {
        if (biomes == null) return;
        biomes.getAllPresent((x, y, z, value) -> consumer.accept(x, y, z, value - 1));
    }

    /// Drops every set biome cell for which `keep` returns false, at biome-cell coordinates (0..3), not
    /// blocks. Returns [#hasBiomes()] afterwards.
    boolean filterBiomes(PosPredicate keep) {
        Palette palette = biomes;
        if (palette != null) {
            // One bit per biome cell (4³ = 64), cleared after the walk rather than during it.
            long[] drop = {0};
            palette.getAllPresent((x, y, z, _) -> {
                if (!keep.test(x, y, z)) drop[0] |= 1L << (x | (y << 2) | (z << 4));
            });

            for (long bits = drop[0]; bits != 0; bits &= bits - 1) {
                int bit = Long.numberOfTrailingZeros(bits);
                palette.set(bit & 3, (bit >> 2) & 3, (bit >> 4) & 3, LEAVE);
            }
        }

        return hasBiomes();
    }

    BufferSection deepCopy() {
        BufferSection out;
        if (blockPalette != null) {
            out = new BufferSection(blockPalette.clone());
        } else {
            out = new BufferSection();
            out.sparse().putAll(sparse());
        }

        if (biomes != null) out.biomes = biomes.clone();
        if (blockEntities != null) {
            out.blockEntities = new Int2ObjectOpenHashMap<>(blockEntities);
            out.blockEntityBytes = blockEntityBytes;
        }
        return out;
    }

    /// Approximate retained heap of this section: the object and its slot in the buffer's section
    /// map, the block palette (or sparse map), the biome palette, and the block entities.
    long sizeBytes() {
        long total = HeapSize.SECTION;
        total += blockPalette != null
            ? PaletteHack.residentBytes(blockPalette)
            : HeapSize.openHashMap(sparse().size(), Integer.BYTES, Short.BYTES);
        if (biomes != null) total += PaletteHack.residentBytes(biomes);
        if (blockEntities != null) {
            total += HeapSize.openHashMap(blockEntities.size(), Integer.BYTES, 4)
                + blockEntityBytes;
        }
        return total;
    }

    private Int2ShortMap sparse() {
        return Objects.requireNonNull(blockSparse, "sparse block repr");
    }

    private void densify() {
        Palette palette = Palette.empty(
            BLOCK_DIM,
            BLOCK_MIN_BITS,
            BLOCK_MAX_BITS,
            BLOCK_DIRECT_BITS
        );
        for (Int2ShortMap.Entry entry : Int2ShortMaps.fastIterable(sparse())) {
            int index = entry.getIntKey();
            palette.set(
                sectionBlockIndexGetX(index),
                sectionBlockIndexGetY(index),
                sectionBlockIndexGetZ(index),
                entry.getShortValue() & 0xFFFF
            );
        }

        this.blockPalette = palette;
        this.blockSparse = null;
    }

}
