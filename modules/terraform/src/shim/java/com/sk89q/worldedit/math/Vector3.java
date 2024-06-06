package com.sk89q.worldedit.math;

public record Vector3(double x, double y, double z) {

    public static final Vector3 ZERO = new Vector3(0, 0, 0);
    public static final Vector3 ONE = new Vector3(1, 1, 1);

    public static Vector3 at(double x, double y, double z) {
        return new Vector3(x, y, z);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public int getBlockX() {
        return (int) x;
    }

    public int getBlockY() {
        return (int) y;
    }

    public int getBlockZ() {
        return (int) z;
    }

    public Vector3 add(double x, double y, double z) {
        return new Vector3(this.x + x, this.y + y, this.z + z);
    }

    public Vector3 add(Vector3 other) {
        return new Vector3(this.x + other.x, this.y + other.y, this.z + other.z);
    }

    public Vector3 subtract(Vector3 other) {
        return new Vector3(this.x - other.x, this.y - other.y, this.z - other.z);
    }

    public Vector3 multiply(double factor) {
        return new Vector3(x * factor, y * factor, z * factor);
    }

    public Vector3 multiply(Vector3 other) {
        return new Vector3(x * other.x, y * other.y, z * other.z);
    }

    public Vector3 divide(double factor) {
        return new Vector3(x / factor, y / factor, z / factor);
    }

    public Vector3 divide(Vector3 other) {
        return new Vector3(x / other.x, y / other.y, z / other.z);
    }

    public double distance(Vector3 other) {
        return Math.sqrt(Math.pow(other.x - x, 2) + Math.pow(other.y - y, 2) + Math.pow(other.z - z, 2));
    }

    public Vector3 normalize() {
        double length = length();
        return new Vector3(x / length, y / length, z / length);
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public Vector3 cross(Vector3 other) {
        return new Vector3(
                y * other.z - z * other.y,
                z * other.x - x * other.z,
                x * other.y - y * other.x
        );
    }

    public BlockVector3 toBlockPoint() {
        return new BlockVector3((int) x, (int) y, (int) z);
    }

    public Vector3 mutX(double x) {
        return new Vector3(x, y, z);
    }

    public Vector3 mutY(double y) {
        return new Vector3(x, y, z);
    }

    public Vector3 mutZ(double z) {
        return new Vector3(x, y, z);
    }

    public Vector3 withX(double x) {
        return new Vector3(x, y, z);
    }

    public Vector3 withY(double y) {
        return new Vector3(x, y, z);
    }

    public Vector3 withZ(double z) {
        return new Vector3(x, y, z);
    }

    public Vector2 toVector2() {
        return new Vector2(x, z);
    }

    public double toYaw() {
        double x = x();
        double z = z();

        double t = Math.atan2(-x, z);
        double tau = 2 * Math.PI;

        return Math.toDegrees(((t + tau) % tau));
    }
}
