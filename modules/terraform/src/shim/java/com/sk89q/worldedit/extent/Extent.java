package com.sk89q.worldedit.extent;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;

public interface Extent {

    BlockState getBlock(BlockVector3 position);
}
