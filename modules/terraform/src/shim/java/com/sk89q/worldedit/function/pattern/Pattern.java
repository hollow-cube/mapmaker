package com.sk89q.worldedit.function.pattern;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;

public interface Pattern {

    BlockState apply(BlockVector3 pos);
}
