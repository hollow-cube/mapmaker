package net.hollowcube.schem;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import net.hollowcube.schem.old.CoordinateUtil;
import net.hollowcube.schem.util.BlockConsumer;
import net.hollowcube.schem.util.VarIntReader;
import net.kyori.adventure.nbt.*;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import static net.hollowcube.schem.old.CoordinateUtil.blockIndex;

@SuppressWarnings("UnstableApiUsage")
public record SpongeSchematic(
        @NotNull CompoundBinaryTag metadata,
        @NotNull Point size,
        @NotNull Point offset,
        @NotNull List<Block> blockPalette,
        @NotNull ByteArrayBinaryTag blockData,
        @NotNull List<String> biomePalette,
        @NotNull ByteArrayBinaryTag biomeData,
        @NotNull Int2ObjectMap<BlockEntityData> blockEntitiesByPos,
        @NotNull List<CompoundBinaryTag> entities
) implements Schematic {
    private static final BlockManager BLOCK_MANAGER = MinecraftServer.getBlockManager();

    public static final ByteArrayBinaryTag EMPTY_BYTE_ARRAY = ByteArrayBinaryTag.byteArrayBinaryTag();
    public static final Schematic EMPTY = new SpongeSchematic(
            CompoundBinaryTag.empty(), Vec.ZERO, Vec.ZERO,
            List.of(), EMPTY_BYTE_ARRAY,
            List.of(), EMPTY_BYTE_ARRAY,
            Int2ObjectMaps.emptyMap(), List.of()
    );

    public SpongeSchematic {
        blockPalette = List.copyOf(blockPalette);
        biomePalette = List.copyOf(biomePalette);
        blockEntitiesByPos = Int2ObjectMaps.unmodifiable(blockEntitiesByPos);
        entities = List.copyOf(entities);
    }

    @Override
    public void forEachBlock(@NotNull Rotation rotation, @NotNull BlockConsumer consumer) {
        var reader = new VarIntReader(this.blockData);
        for (int y = 0; y < size().y(); y++) {
            for (int z = 0; z < size().z(); z++) {
                for (int x = 0; x < size().x(); x++) {
                    var block = blockPalette.get(reader.next());
                    var blockEntity = blockEntitiesByPos.get(blockIndex(size, x, y, z));
                    if (blockEntity != null) {
                        block = block.withHandler(BLOCK_MANAGER.getHandlerOrDummy(blockEntity.id()))
                                .withNbt(blockEntity.data());
                    }

                    consumer.accept(
                            CoordinateUtil.rotatePos(offset.add(x, y, z), rotation),
                            CoordinateUtil.rotateBlock(block, rotation)
                    );
                }
            }
        }
    }

    @Override
    public @Nullable String name() {
        var name = metadata.get("Name");
        if (name != null && name.type() == BinaryTagTypes.STRING)
            return ((StringBinaryTag) name).value();
        return null;
    }

    @Override
    public @Nullable String author() {
        var name = metadata.get("Author");
        if (name != null && name.type() == BinaryTagTypes.STRING)
            return ((StringBinaryTag) name).value();
        return null;
    }

    @Override
    public @Nullable Instant createdAt() {
        var name = metadata.get("Date");
        if (name != null && name.type() == BinaryTagTypes.LONG)
            return Instant.ofEpochMilli(((LongBinaryTag) name).value());
        return null;
    }

    @Override
    public boolean hasBlockData() {
        return (blockData.size() > 0 && !blockPalette.isEmpty()) || !blockEntitiesByPos.isEmpty();
    }

    @Override
    public @NotNull Collection<BlockEntityData> blockEntities() {
        return blockEntitiesByPos.values();
    }

    @Override
    public boolean hasBiomeData() {
        return biomeData.size() > 0 && !biomePalette.isEmpty();
    }
}
