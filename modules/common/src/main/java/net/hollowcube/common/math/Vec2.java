package net.hollowcube.common.math;

/// A plain 2D `(x, y)` vector supporting a single planar rotation. Used where the two axes
/// don't map to a world axis (e.g. a `(horizontal, vertical)` power pair), so Minestom's 3D
/// `Vec` with its axis-specific rotations doesn't fit.
public record Vec2(double x, double y) {

    public Vec2 rotate(double angleRadians) {
        double cos = Math.cos(angleRadians), sin = Math.sin(angleRadians);
        return new Vec2(x * cos - y * sin, y * cos + x * sin);
    }
}
