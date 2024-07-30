package net.hollowcube.mapmaker.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class CoordinateUtil {
    private CoordinateUtil() {
    }

    public static boolean intersects(@NotNull Point min1, @NotNull Point max1, @NotNull Point min2, @NotNull Point max2) {
        return min1.x() <= max2.x() && max1.x() >= min2.x() &&
                min1.y() <= max2.y() && max1.y() >= min2.y() &&
                min1.z() <= max2.z() && max1.z() >= min2.z();
    }

    // pos1 = position of bb, bb1 = bounding box of entity
    public static boolean intersects(@NotNull Point pos1, @NotNull BoundingBox bb1, @NotNull Point pos2, @NotNull BoundingBox bb2) {
        return pos1.x() + bb1.minX() <= pos2.x() + bb2.maxX() && pos1.x() + bb1.maxX() >= pos2.x() + bb2.minX() &&
                pos1.y() + bb1.minY() <= pos2.y() + bb2.maxY() && pos1.y() + bb1.maxY() >= pos2.y() + bb2.minY() &&
                pos1.z() + bb1.minZ() <= pos2.z() + bb2.maxZ() && pos1.z() + bb1.maxZ() >= pos2.z() + bb2.minZ();
    }

    public static boolean intersects(@NotNull Entity entity1, @NotNull Entity entity2) {
        return intersects(entity1.getPosition(), entity1.getBoundingBox(), entity2.getPosition(), entity2.getBoundingBox());
    }

    public static boolean isBetween(@NotNull Point min, @NotNull Point max, @NotNull Point p) {
        return p.x() >= min.x() && p.x() <= max.x() &&
                p.y() >= min.y() && p.y() <= max.y() &&
                p.z() >= min.z() && p.z() <= max.z();
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

    public static @NotNull List<Component> asTranslationArgs(@NotNull Point point) {
        return List.of(
                Component.text(NumberUtil.format(point.x(), 2)).hoverEvent(Component.text(point.x(), NamedTextColor.WHITE)),
                Component.text(NumberUtil.format(point.y(), 2)).hoverEvent(Component.text(point.y(), NamedTextColor.WHITE)),
                Component.text(NumberUtil.format(point.z(), 2)).hoverEvent(Component.text(point.z(), NamedTextColor.WHITE))
        );
    }

    public static @NotNull List<Component> asTranslationArgs(@NotNull Pos pos) {
        var pointArgs = new ArrayList<>(asTranslationArgs((Point) pos));
        pointArgs.add(Component.text(NumberUtil.format(pos.yaw(), 2)).hoverEvent(Component.text(pos.yaw(), NamedTextColor.WHITE)));
        pointArgs.add(Component.text(NumberUtil.format(pos.pitch(), 2)).hoverEvent(Component.text(pos.pitch(), NamedTextColor.WHITE)));
        return pointArgs;
    }
}
