package net.hollowcube.schem;

import net.hollowcube.schem.util.BlockConsumer;
import net.kyori.adventure.nbt.ByteArrayBinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public record Structure(
        @NotNull Point size,
        @NotNull List<BlockInfo> blocks,
        @NotNull List<Block[]> palettes,
        @NotNull List<CompoundBinaryTag> entities
) implements Schematic {

    public record BlockInfo(
            @NotNull Point pos,
            int paletteIndex,
            @Nullable BlockEntityData blockEntity
    ) {
    }

    @Override
    public void forEachBlock(@NotNull Rotation rotation, @NotNull BlockConsumer consumer) {

    }

    @Override
    public boolean hasBlockData() {
        return true;
    }

    @Override
    public @NotNull List<Block> blockPalette() {
        // All of this logic just ensures the palette contains air.
        int airIndex = -1;
        var palette = palettes.get(0);
        for (int i = 0; i < palette.length; i++) {
            if (palette[i] == Block.AIR) {
                airIndex = i;
                break;
            }
        }
        if (airIndex == -1) {
            palette = Arrays.copyOf(palette, palette.length + 1);
            palette[palette.length - 1] = Block.AIR;
        }
        return List.of(palette);
    }

    @Override
    public @NotNull ByteArrayBinaryTag blockData() {
        // Figure out the index of the air entry, or add one at the end
        int airIndex = -1;
        var palette = palettes.get(0);
        for (int i = 0; i < palette.length; i++) {
            if (palette[i] == Block.AIR) {
                airIndex = i;
                break;
            }
        }
        if (airIndex == -1) airIndex = palette.length;

        // Create the palette array
        var indices = new int[size.blockX() * size.blockY() * size.blockZ()];
        Arrays.fill(indices, airIndex);
        for (var bi : blocks) {
            var index = (int) (bi.pos.blockX() + bi.pos.blockZ() * size.x() + bi.pos.blockY() * size.x() * size.z());
            indices[index] = bi.paletteIndex;
        }
        return ByteArrayBinaryTag.byteArrayBinaryTag(NetworkBuffer.makeArray(buffer -> {
            for (var paletteIndex : indices) {
                buffer.write(NetworkBuffer.VAR_INT, paletteIndex);
            }
        }));
    }

    @Override
    public @NotNull List<BlockEntityData> blockEntities() {
        var blockEntities = new ArrayList<BlockEntityData>();
        for (var blockInfo : blocks) {
            if (blockInfo.blockEntity == null) continue;
            blockEntities.add(blockInfo.blockEntity);
        }
        return Collections.synchronizedList(blockEntities);
    }
}
