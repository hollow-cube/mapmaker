package com.sk89q.worldedit.regions.factory;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.EllipsoidRegion;
import com.sk89q.worldedit.regions.Region;

public class SphereRegionFactory {

    public Region createCenteredAt(BlockVector3 position, double size) {
        return new EllipsoidRegion(null, position, Vector3.at(size, size, size));
    }
}
