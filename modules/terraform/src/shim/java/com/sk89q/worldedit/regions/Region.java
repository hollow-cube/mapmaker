package com.sk89q.worldedit.regions;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;

public interface Region extends Iterable<BlockVector3> {

    BlockVector3 getMaximumPoint();

    BlockVector3 getMinimumPoint();

    default Vector3 getCenter() {
        return getMinimumPoint().add(getMaximumPoint()).divide(2).toVector3();
    }

    default int getWidth() {
        return getMaximumPoint().getX() - getMinimumPoint().getX() + 1;
    }

    default int getHeight() {
        return getMaximumPoint().getY() - getMinimumPoint().getY() + 1;
    }

    default int getLength() {
        return getMaximumPoint().getZ() - getMinimumPoint().getZ() + 1;
    }

    boolean contains(int x, int y, int z);

    default boolean contains(BlockVector3 blockVector3) {
        return contains(blockVector3.getX(), blockVector3.getY(), blockVector3.getZ());
    }
}
