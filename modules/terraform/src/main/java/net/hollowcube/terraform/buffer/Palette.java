package net.hollowcube.terraform.buffer;

import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.Nullable;

public sealed interface Palette permits NaivePalette {
    int UNSET = -1;

    @Nullable Block get(int x, int y, int z);

    void set(int x, int y, int z, int value);

    void set(int x, int y, int z, @Nullable Block value);

    long sizeBytes();

}
