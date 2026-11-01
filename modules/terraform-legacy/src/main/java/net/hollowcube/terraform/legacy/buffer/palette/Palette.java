package net.hollowcube.terraform.legacy.buffer.palette;

import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.Nullable;

public interface Palette {

    @Nullable Block get(int x, int y, int z);

    long sizeBytes();

}
