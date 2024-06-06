package com.sk89q.worldedit.world.registry;

import net.minestom.server.instance.block.Block;

public interface BlockMaterial {

    Block block();

    default boolean isAir() {
        return block().isAir();
    }

    default boolean isSolid() {
        return block().isSolid();
    }

    default boolean isLiquid() {
        return block().isLiquid();
    }
}
