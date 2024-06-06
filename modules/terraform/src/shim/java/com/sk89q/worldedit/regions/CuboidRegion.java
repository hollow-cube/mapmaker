package com.sk89q.worldedit.regions;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.iterator.RegionIterator;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;

public class CuboidRegion implements Region {

    //FAWE start
    private int minX;
    private int minY;
    private int minZ;
    private int maxX;
    private int maxY;
    private int maxZ;
    //FAWE end
    private BlockVector3 pos1;
    private BlockVector3 pos2;

    public CuboidRegion(BlockVector3 pos1, BlockVector3 pos2) {
        checkNotNull(pos1);
        checkNotNull(pos2);
        this.pos1 = pos1;
        this.pos2 = pos2;
        recalculate();
    }

    void recalculate() {
        if (pos1 == null || pos2 == null) {
            return;
        }
//        pos1 = pos1.clampY(getWorldMinY(), getWorldMaxY());
//        pos2 = pos2.clampY(getWorldMinY(), getWorldMaxY());
        minX = Math.min(pos1.x(), pos2.x());
        minY = Math.min(pos1.y(), pos2.y());
        minZ = Math.min(pos1.z(), pos2.z());
        maxX = Math.max(pos1.x(), pos2.x());
        maxY = Math.max(pos1.y(), pos2.y());
        maxZ = Math.max(pos1.z(), pos2.z());
    }

    @Override
    public BlockVector3 getMaximumPoint() {
        return BlockVector3.at(maxX, maxY, maxZ);
    }

    @Override
    public BlockVector3 getMinimumPoint() {
        return BlockVector3.at(minX, minY, minZ);
    }

    @Override
    public boolean contains(int x, int y, int z) {
        return x >= this.minX && x <= this.maxX && z >= this.minZ && z <= this.maxZ && y >= this.minY && y <= this.maxY;
    }

    @Override
    public @NotNull Iterator<BlockVector3> iterator() {
        return new RegionIterator(this);
    }
}
