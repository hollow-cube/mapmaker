package com.sk89q.worldedit.function.mask;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;

public class SolidBlockMask implements Mask {
    private final Extent extent;

    public SolidBlockMask(Extent extent) {
        this.extent = extent;
    }

    @Override
    public boolean test(BlockVector3 blockPosition) {
        return extent.getBlock(blockPosition).block().isSolid();
    }
}
