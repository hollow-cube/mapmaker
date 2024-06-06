package com.sk89q.worldedit.function.mask;

import com.sk89q.worldedit.math.BlockVector3;

public interface Mask {

    boolean test(BlockVector3 blockPosition);
}
