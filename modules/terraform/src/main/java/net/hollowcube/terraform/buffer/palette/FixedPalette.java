package net.hollowcube.terraform.buffer.palette;

import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.Nullable;

public record FixedPalette(int value) implements Palette {
    @Override
    public @Nullable Block get(int x, int y, int z) {
        if (value == Palette.UNSET) return null;
        return Block.fromStateId((short) value);
    }

    @Override
    public long sizeBytes() {
        return 4;
    }
}
