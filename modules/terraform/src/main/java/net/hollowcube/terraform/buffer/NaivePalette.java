package net.hollowcube.terraform.buffer;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.Nullable;

final class NaivePalette implements Palette {
    private static final int SIZE = 16;

    private final int[] palette = new int[SIZE * SIZE * SIZE];

    private final Int2ObjectMap<Block> cachedBlocks = new Int2ObjectArrayMap<>();

    @Override
    public @Nullable Block get(int x, int y, int z) {
        int index = getIndex(x, y, z);
        int stateId = palette[index] - 1;
        if (stateId == UNSET) return null;
        return cachedBlocks.getOrDefault(index, Block.fromStateId((short) stateId));
    }

    @Override
    public void set(int x, int y, int z, int value) {
        palette[getIndex(x, y, z)] = value + 1;
    }

    @Override
    public void set(int x, int y, int z, @Nullable Block value) {
        int index = getIndex(x, y, z);
        if (value == null) {
            palette[index] = UNSET;
        } else {
            palette[index] = value.stateId() + 1;
            if (value.handler() != null || value.hasNbt())
                cachedBlocks.put(index, value);
        }
    }

    @Override
    public long sizeBytes() {
        return (long) palette.length * Integer.BYTES;
    }

    private static int getIndex(int x, int y, int z) {
        return x + SIZE * (y + SIZE * z);
    }
}
