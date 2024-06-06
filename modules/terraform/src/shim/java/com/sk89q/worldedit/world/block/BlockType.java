package com.sk89q.worldedit.world.block;

import com.sk89q.worldedit.world.registry.BlockMaterial;
import net.minestom.server.instance.block.Block;

public record BlockType(Block block) {

    public BlockMaterial getMaterial() {
        return () -> block;
    }

    public BlockState getDefaultState() {
        return new BlockState(Block.fromBlockId(block.id()));
    }
}
