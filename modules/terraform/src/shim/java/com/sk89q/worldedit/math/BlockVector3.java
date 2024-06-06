package com.sk89q.worldedit.math;

import net.minestom.server.coordinate.Point;

public record BlockVector3(int x, int y, int z) {

    public static final BlockVector3 ZERO = new BlockVector3(0, 0, 0);
    public static final BlockVector3 ONE = new BlockVector3(1, 1, 1);

    public BlockVector3(Point pos) {
        this(pos.blockX(), pos.blockY(), pos.blockZ());
    }

    public static BlockVector3 at(double x, double y, double z) {
        return new BlockVector3((int) x, (int) y, (int) z);
    }

    public static BlockVector3 at(int x, int y, int z) {
        return new BlockVector3(x, y, z);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public int getBlockX() {
        return x;
    }

    public int getBlockY() {
        return y;
    }

    public int getBlockZ() {
        return z;
    }

    public BlockVector3 add(int x, int y, int z) {
        return new BlockVector3(this.x + x, this.y + y, this.z + z);
    }

    public BlockVector3 add(BlockVector3 other) {
        return new BlockVector3(this.x + other.x, this.y + other.y, this.z + other.z);
    }

    public BlockVector3 subtract(int x, int y, int z) {
        return new BlockVector3(this.x - x, this.y - y, this.z - z);
    }

    public BlockVector3 subtract(BlockVector3 other) {
        return new BlockVector3(this.x - other.x, this.y - other.y, this.z - other.z);
    }

    public BlockVector3 multiply(double factor) {
        return new BlockVector3((int) (x * factor), (int) (y * factor), (int) (z * factor));
    }

    public BlockVector3 divide(double factor) {
        return new BlockVector3((int) (x / factor), (int) (y / factor), (int) (z / factor));
    }

    public BlockVector3 withY(int y) {
        return new BlockVector3(x, y, z);
    }

    public double distance(BlockVector3 other) {
        return Math.sqrt(distanceSq(other));
    }

    public int distanceSq(BlockVector3 other) {
        int dx = other.x() - x();
        int dy = other.y() - y();
        int dz = other.z() - z();
        return dx * dx + dy * dy + dz * dz;
    }

    public boolean equals(BlockVector3 other) {
        return x == other.x && y == other.y && z == other.z;
    }

    public Vector3 toVector3() {
        return Vector3.at(x, y, z);
    }
}
