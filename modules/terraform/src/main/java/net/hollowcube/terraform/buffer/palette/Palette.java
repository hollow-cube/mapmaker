package net.hollowcube.terraform.buffer.palette;

import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Palette {

    /**
     * Creates the default palette for blocks expected to be filled afterward.
     *
     * @return A writable palette
     */
    static @NotNull Palette.Mutable blocks() {
        return new NaivePalette(); //todo this is garbage
    }

    int UNSET = -1;

    @Nullable Block get(int x, int y, int z);

    long sizeBytes();

    interface Mutable extends Palette {
        void set(int x, int y, int z, int value);

        void set(int x, int y, int z, @Nullable Block value);
    }

}
