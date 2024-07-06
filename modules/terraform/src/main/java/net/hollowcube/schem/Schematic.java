package net.hollowcube.schem;

import net.hollowcube.schem.util.BlockConsumer;
import net.kyori.adventure.nbt.ByteArrayBinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.batch.BatchOption;
import net.minestom.server.instance.batch.RelativeBlockBatch;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * <p>An publicly-immutable copy of any loaded schematic. This interface defines the basic requirements for any schematic, however
 * this may not be a complete representation of the internal details of a schematic. For example, {@link Structure}
 * supports multiple palettes, however this information is lost in the generic Schematic api (the first palette is used).</p>
 *
 * <p>Any schematic may be written to any {@link net.hollowcube.schem.writer.SchematicWriter}, no matter the type. If
 * the type does not match the writer, it will be converted as it is written (note that this conversion could be a
 * relatively expensive process.</p>
 */
public interface Schematic {

    static @NotNull Schematic empty() {
        return SpongeSchematic.EMPTY;
    }

    default @NotNull CompoundBinaryTag metadata() {
        return CompoundBinaryTag.empty();
    }

    /**
     * Returns the name of the schematic, if available.
     */
    default @Nullable String name() {
        return null;
    }
    /**
     * Returns the author of the schematic, if available.
     */
    default @Nullable String author() {
        return null;
    }
    /**
     * Returns the creation time of the schematic, if available.
     */
    default @Nullable Instant createdAt() {
        return null;
    }

    @NotNull Point size();
    default @NotNull Point offset() {
        return Vec.ZERO;
    }

    // Application functions

    default void forEachBlock(@NotNull BlockConsumer consumer) {
        forEachBlock(Rotation.NONE, consumer);
    }

    void forEachBlock(@NotNull Rotation rotation, @NotNull BlockConsumer consumer);

    default @NotNull RelativeBlockBatch createBatch() {
        return createBatch(Rotation.NONE, null);
    }
    default @NotNull RelativeBlockBatch createBatch(@NotNull UnaryOperator<@Nullable Block> blockTransformer) {
        return createBatch(Rotation.NONE, blockTransformer);
    }
    default @NotNull RelativeBlockBatch createBatch(@NotNull Rotation rotation) {
        return createBatch(rotation, null);
    }
    default @NotNull RelativeBlockBatch createBatch(@NotNull Rotation rotation, @Nullable UnaryOperator<@Nullable Block> blockTransformer) {
        RelativeBlockBatch batch = new RelativeBlockBatch(new BatchOption().setCalculateInverse(true));
        forEachBlock(rotation, (pos, block) -> {
            var resultBlock = blockTransformer == null ? block : blockTransformer.apply(block);
            if (resultBlock != null) batch.setBlock(pos, resultBlock);
        });
        return batch;
    }

    // Raw data access below
    // Generally intended for serialization, care should be used when calling.

    /**
     * Returns true if the schematic has block data (including block entities), false otherwise.
     */
    default boolean hasBlockData() {
        return false;
    }
    /**
     * Returns the block palette for the entire schematic, zero indexed with no empty spaces.
     *
     * <p>Note: the value may be computed when called so may be uncached and expensive. It is
     * wise to cache the result if this will be called many times, although this is largely an
     * internal api used for serialization.</p>
     *
     * @return The (computed) block palette for the schematic
     */
    default @NotNull List<Block> blockPalette() {
        return List.of();
    }
    /**
     * Returns the block data for the entire schematic. The format is (size.x * size.y * size.z) var ints
     * in a row, each corresponding to an entry in {@link #blockPalette()}.
     *
     * <p>Note: the value may be computed when called so may be uncached and expensive. It is
     * wise to cache the result if this will be called many times, although this is largely an
     * internal api used for serialization.</p>
     *
     * @return The (computed) block palette for the schematic
     */
    default @NotNull ByteArrayBinaryTag blockData() {
        return SpongeSchematic.EMPTY_BYTE_ARRAY;
    }
    default @NotNull Collection<BlockEntityData> blockEntities() {
        return List.of();
    }

    default boolean hasBiomeData() {
        return false;
    }
    default @NotNull List<String> biomePalette() {
        return List.of();
    }
    default @NotNull ByteArrayBinaryTag biomeData() {
        return SpongeSchematic.EMPTY_BYTE_ARRAY;
    }

    default @NotNull List<CompoundBinaryTag> entities() {
        return List.of();
    }
}
