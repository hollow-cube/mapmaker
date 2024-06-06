package com.thevoxelbox.voxelsniper.brush.property;

import com.sk89q.worldedit.world.block.BlockState;
import net.minestom.server.instance.block.Block;

public class BrushPattern {

    public BlockState asBlockState() {
        return new BlockState(Block.DIAMOND_BLOCK); //todo
    }
}
