package com.thevoxelbox.voxelsniper.util.material;

import com.sk89q.worldedit.world.block.BlockType;

public class Materials {

    public static boolean isEmpty(BlockType block) {
        return block.block().isAir();
    }

    public static boolean isLiquid(BlockType block) {
        return block.block().isLiquid();
    }
}
