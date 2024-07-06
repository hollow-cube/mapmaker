package net.hollowcube.schem;

import net.hollowcube.schem.util.BlockConsumer;
import net.kyori.adventure.nbt.ByteArrayBinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.IntArrayBinaryTag;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record AxiomBlueprint(
        @NotNull CompoundBinaryTag header,
        @NotNull ByteArrayBinaryTag thumbnail,
        @NotNull Point size,
        @NotNull List<Block> palette,
        @NotNull List<SectionData> sections
) implements Schematic {
    public static final int MAGIC_NUMBER = 0xAE5BB36;
    public static final int BLOCK_PALETTE_SIZE = 4096;

    public record SectionData(
            @NotNull Point chunkPos,
            @NotNull List<Block> palette,
            // Null means all one palette
            @Nullable IntArrayBinaryTag blockData
    ) {
    }

    @Override
    public void forEachBlock(@NotNull Rotation rotation, @NotNull BlockConsumer consumer) {

    }

    @Override
    public @NotNull CompoundBinaryTag metadata() {
        return header;
    }

    public @NotNull ByteArrayBinaryTag thumbnail() {
        return thumbnail;
    }

    @Override
    public @NotNull String name() {
        return header.getString("Name");
    }

    @Override
    public @NotNull String author() {
        return header.getString("Author");
    }

    @Override
    public @NotNull Point size() {
        return size;
    }

    @Override
    public boolean hasBlockData() {
        return true;
    }

    @Override
    public @NotNull List<Block> blockPalette() {
        return List.of();
    }

    @Override
    public @NotNull ByteArrayBinaryTag blockData() {
        return SpongeSchematic.EMPTY_BYTE_ARRAY;
    }

}
