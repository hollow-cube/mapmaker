package com.thevoxelbox.voxelsniper.brush;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;

public interface Brush {

    BlockState getBlock(BlockVector3 pos);

    EditSession getEditSession();
}
