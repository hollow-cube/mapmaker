package net.hollowcube.schem.builder;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.hollowcube.schem.BlockEntityData;
import net.hollowcube.schem.Schematic;
import net.hollowcube.schem.SpongeSchematic;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.ByteArrayBinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.hollowcube.schem.old.CoordinateUtil.blockIndex;

@SuppressWarnings("UnstableApiUsage")
final class SizedSchematicBuilder implements SchematicBuilder {
    private final CompoundBinaryTag.Builder metadata = CompoundBinaryTag.builder();
    private final Int2ObjectMap<BlockEntityData> blockEntities = new Int2ObjectOpenHashMap<>();
    private final IntList palette = new IntArrayList(1);
    private final short[] blocks;
    private final Point size;

    private Point offset = Vec.ZERO;

    SizedSchematicBuilder(@NotNull Point size) {
        palette.add(Block.AIR.stateId());
        this.blocks = new short[size.blockX() * size.blockY() * size.blockZ()];
        this.size = size;
    }

    @Override
    public void metadata(@NotNull String key, @NotNull BinaryTag value) {
        metadata.put(key, value);
    }

    @Override
    public void block(@NotNull Point point, @NotNull Block block) {
        block(point.blockX(), point.blockY(), point.blockZ(), block);
    }

    @Override
    public void block(int x, int y, int z, @NotNull Block block) {
        var paletteIndex = palette.indexOf(block.stateId());
        if (paletteIndex == -1) {
            paletteIndex = palette.size();
            palette.add(block.stateId());
        }
        var relPos = new Vec(x, y, z).sub(offset);
        var blockIndex = blockIndex(size, relPos);
        blocks[blockIndex] = (short) paletteIndex;

        var blockHandler = block.handler();
        if (blockHandler != null) {
            var blockEntityId = blockHandler.getNamespaceId().asString();
            var blockEntityData = Objects.requireNonNullElse(block.nbt(), CompoundBinaryTag.empty());
            blockEntities.put(blockIndex, new BlockEntityData(blockEntityId, relPos, blockEntityData));
        }
    }

    @Override
    public void offset(@NotNull Point point) {
        this.offset = point;
    }

    @Override
    public @NotNull Schematic build() {
        var paletteBlocks = new ArrayList<Block>(palette.size());
        for (var stateId : palette) paletteBlocks.add(Block.fromStateId(stateId));

        var blockData = ByteArrayBinaryTag.byteArrayBinaryTag(NetworkBuffer.makeArray(buffer -> {
            for (var paletteIdx : blocks)
                buffer.write(NetworkBuffer.VAR_INT, (int) paletteIdx);
        }));

        return new SpongeSchematic(
                metadata.build(), size, offset,
                paletteBlocks, blockData,
                List.of(), SpongeSchematic.EMPTY_BYTE_ARRAY,
                blockEntities, List.of()
        );
    }
}
