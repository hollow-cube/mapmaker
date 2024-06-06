package com.sk89q.worldedit.world;

import net.minestom.server.instance.Instance;

public record WorldImpl(Instance instance) implements World {
    @Override
    public int getMinY() {
        return instance.getCachedDimensionType().minY();
    }

    @Override
    public int getMaxY() {
        return instance.getCachedDimensionType().maxY();
    }
}
