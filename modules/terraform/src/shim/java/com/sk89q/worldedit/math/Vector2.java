package com.sk89q.worldedit.math;

public record Vector2(double x, double z) {

    public static Vector2 at(double x, double z) {
        return new Vector2(x, z);
    }

    public double getX() {
        return x;
    }

    public double getZ() {
        return z;
    }

    public double length() {
        return Math.sqrt(x * x + z * z);
    }

    public double distance(Vector2 other) {
        return Math.sqrt(Math.pow(other.x - x, 2) + Math.pow(other.z - z, 2));
    }
}
