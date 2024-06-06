package com.thevoxelbox.voxelsniper.performer;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.world.block.BlockState;

public interface Performer {

    void perform(EditSession s, int x, int y, int z, BlockState block);

}
