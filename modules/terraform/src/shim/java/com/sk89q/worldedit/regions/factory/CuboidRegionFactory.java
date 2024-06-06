package com.sk89q.worldedit.regions.factory;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;

public class CuboidRegionFactory {

    public Region createCenteredAt(BlockVector3 position, double size) {
        return fromCenter(position, (int) size);
    }

    public static CuboidRegion fromCenter(BlockVector3 origin, int apothem) {
//        checkNotNull(origin);
//        checkArgument(apothem >= 0, "apothem => 0 required");
        BlockVector3 size = BlockVector3.ONE.multiply(apothem);
        return new CuboidRegion(origin.subtract(size), origin.add(size));
    }
}
