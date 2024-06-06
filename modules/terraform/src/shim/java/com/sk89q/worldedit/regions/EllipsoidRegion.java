package com.sk89q.worldedit.regions;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.iterator.RegionIterator;
import com.sk89q.worldedit.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public class EllipsoidRegion implements Region {

    private final World world;
    private final BlockVector3 center;
    private final Vector3 radius;

    private Vector3 radiusSqr;
    private Vector3 inverseRadiusSqr;
    private int radiusLengthSqr;
    private boolean sphere;

    public EllipsoidRegion(World world, BlockVector3 center, Vector3 radius) {
        this.world = world;
        this.center = center;
        this.radius = radius;

        this.radiusSqr = radius.multiply(radius);
        this.radiusLengthSqr = (int) radiusSqr.x();
        this.sphere = radius.y() == radius.x() && radius.x() == radius.z();
        this.inverseRadiusSqr = Vector3.ONE.divide(radiusSqr);
    }

    @Override
    public @NotNull Iterator<BlockVector3> iterator() {
        return new RegionIterator(this);
    }

    public Vector3 getCenter() {
        return center.toVector3();
    }

    public Vector3 getRadius() {
        return radius;
    }

    @Override
    public BlockVector3 getMinimumPoint() {
        return center.toVector3().subtract(getRadius()).toBlockPoint();
    }

    @Override
    public BlockVector3 getMaximumPoint() {
        return center.toVector3().add(getRadius()).toBlockPoint();
    }

    public double getWorldMaxY() {
        return world == null ? Integer.MAX_VALUE : world.getMaxY();
    }

    @Override
    public boolean contains(int x, int y, int z) {
        int cx = x - center.x();
        int cx2 = cx * cx;
        if (cx2 > radiusSqr.getBlockX()) {
            return false;
        }
        int cz = z - center.z();
        int cz2 = cz * cz;
        if (cz2 > radiusSqr.getBlockZ()) {
            return false;
        }
        int cy = y - center.y();
        int cy2 = cy * cy;
        if (radiusSqr.getBlockY() < getWorldMaxY() && cy2 > radiusSqr.getBlockY()) {
            return false;
        }
        if (sphere) {
            return cx2 + cy2 + cz2 <= radiusLengthSqr;
        }
        double cxd = cx2 * inverseRadiusSqr.x();
        double cyd = cy2 * inverseRadiusSqr.y();
        double czd = cz2 * inverseRadiusSqr.z();
        return cxd + cyd + czd <= 1;
    }
}
