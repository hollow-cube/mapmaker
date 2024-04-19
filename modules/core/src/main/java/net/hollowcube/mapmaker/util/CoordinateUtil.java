package net.hollowcube.mapmaker.util;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.NotNull;

public final class CoordinateUtil {
    private CoordinateUtil() {
    }

    public static @NotNull Point abs(@NotNull Point point) {
        return new Vec(Math.abs(point.x()), Math.abs(point.y()), Math.abs(point.z()));
    }

    public static @NotNull Point floor(@NotNull Point point) {
        return new Vec(point.blockX(), point.blockY(), point.blockZ());
    }

    public static @NotNull Point min(@NotNull Point a, @NotNull Point b) {
        return new Vec(
                Math.min(a.x(), b.x()),
                Math.min(a.y(), b.y()),
                Math.min(a.z(), b.z())
        );
    }

    public static @NotNull Point max(@NotNull Point a, @NotNull Point b) {
        return new Vec(
                Math.max(a.x(), b.x()),
                Math.max(a.y(), b.y()),
                Math.max(a.z(), b.z())
        );
    }

    public static @NotNull Vec lerp(Point zero, Point one, float t) {
        return new Vec(lerp(zero.x(), one.x(), t), lerp(zero.y(), one.y(), t), lerp(zero.z(), one.z(), t));
    }

    public static @NotNull Pos lerp(Pos zero, Pos one, float t) {
        return new Pos(
                lerp(zero.x(), one.x(), t),
                lerp(zero.y(), one.y(), t),
                lerp(zero.z(), one.z(), t),
                lerp(zero.yaw(), one.yaw(), t),
                lerp(zero.pitch(), one.pitch(), t)
        );
    }

    public static double lerp(double zero, double one, float t) {
        return zero + (one - zero) * t;
    }

    private static float lerp(float zero, float one, float t) {
        return zero + (one - zero) * t;
    }
}
