package net.hollowcube.common.physics;

import net.minestom.server.collision.BoundingBox;
import net.minestom.server.collision.Shape;
import net.minestom.server.collision.ShapeImpl;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.function.ToDoubleFunction;

/// Coverage and clipping queries over a shape's boxes, which Minestom has no equivalent of. Boxes
/// here are absolute: a [BoundingBox] whose relative start is its minimum corner in the world.
@NotNullByDefault
public final class Shapes {

    public record Hit(Point position, BlockFace face) {
    }

    public static BoundingBox absolute(Point position, BoundingBox box) {
        return BoundingBox.fromPoints(position.add(box.relativeStart()), position.add(box.relativeEnd()));
    }

    public static List<BoundingBox> boxes(Shape shape, Point offset) {
        return switch (shape) {
            case ShapeImpl impl -> impl.boundingBoxes().stream().map(box -> absolute(offset, box)).toList();
            case BoundingBox box -> List.of(absolute(offset, box));
            default -> List.of(BoundingBox.fromPoints(offset.add(shape.relativeStart()), offset.add(shape.relativeEnd())));
        };
    }

    /// The bounds of every box of `shape`, which the shape itself already knows, or null if it has none.
    public static @Nullable BoundingBox bounds(Shape shape, Point offset) {
        if (shape.relativeStart().equals(shape.relativeEnd())) return null;
        return BoundingBox.fromPoints(offset.add(shape.relativeStart()), offset.add(shape.relativeEnd()));
    }

    public static boolean intersects(BoundingBox a, BoundingBox b) {
        return a.intersectBox(Vec.ZERO, b);
    }

    public static boolean containsPoint(BoundingBox box, Point point) {
        return containsPoint(box, point.x(), point.y(), point.z());
    }

    public static boolean contains(BoundingBox outer, BoundingBox inner) {
        return inner.minX() >= outer.minX() && inner.minY() >= outer.minY() && inner.minZ() >= outer.minZ()
               && inner.maxX() <= outer.maxX() && inner.maxY() <= outer.maxY() && inner.maxZ() <= outer.maxZ();
    }

    /// The box shrunk by one ulp on every side, so a query on it never picks up the blocks it only touches.
    public static BoundingBox deflated(BoundingBox box) {
        return BoundingBox.fromPoints(
            new Vec(Math.nextUp(box.minX()), Math.nextUp(box.minY()), Math.nextUp(box.minZ())),
            new Vec(Math.nextDown(box.maxX()), Math.nextDown(box.maxY()), Math.nextDown(box.maxZ()))
        );
    }

    /// Whether the union of `boxes` fully covers `target`, by splitting target along every box edge
    /// and checking that each resulting cell lies inside some box.
    public static boolean covers(List<BoundingBox> boxes, BoundingBox target) {
        var xs = edges(boxes, target.minX(), target.maxX(), BoundingBox::minX, BoundingBox::maxX);
        var ys = edges(boxes, target.minY(), target.maxY(), BoundingBox::minY, BoundingBox::maxY);
        var zs = edges(boxes, target.minZ(), target.maxZ(), BoundingBox::minZ, BoundingBox::maxZ);
        for (int i = 0; i < xs.length - 1; i++) {
            double cx = (xs[i] + xs[i + 1]) / 2;
            for (int j = 0; j < ys.length - 1; j++) {
                double cy = (ys[j] + ys[j + 1]) / 2;
                for (int k = 0; k < zs.length - 1; k++) {
                    double cz = (zs[k] + zs[k + 1]) / 2;
                    boolean covered = false;
                    for (var box : boxes) {
                        if (containsPoint(box, cx, cy, cz)) {
                            covered = true;
                            break;
                        }
                    }
                    if (!covered) return false;
                }
            }
        }
        return true;
    }

    /// The first face entered going from `from` to `to`, or `from` itself when it already lies
    /// inside a box, facing back the way the segment came.
    public static @Nullable Hit clip(List<BoundingBox> boxes, Point from, Point to) {
        var diff = Vec.fromPoint(to.sub(from));
        if (boxes.isEmpty() || diff.isZero()) return null;

        for (var box : boxes) {
            if (containsPoint(box, from)) return new Hit(from, nearestFace(diff).getOppositeFace());
        }

        Hit closest = null;
        double closestT = Double.POSITIVE_INFINITY;
        for (var box : boxes) {
            double enter = 0, exit = 1;
            BlockFace enterFace = null;
            boolean miss = false;
            for (int axis = 0; axis < 3 && !miss; axis++) {
                double start = axis == 0 ? from.x() : axis == 1 ? from.y() : from.z();
                double delta = axis == 0 ? diff.x() : axis == 1 ? diff.y() : diff.z();
                double lo = axis == 0 ? box.minX() : axis == 1 ? box.minY() : box.minZ();
                double hi = axis == 0 ? box.maxX() : axis == 1 ? box.maxY() : box.maxZ();
                if (Math.abs(delta) < Point.EPSILON) {
                    if (start < lo || start > hi) miss = true;
                    continue;
                }
                double tNear = ((delta > 0 ? lo : hi) - start) / delta;
                double tFar = ((delta > 0 ? hi : lo) - start) / delta;
                if (tNear > enter) {
                    enter = tNear;
                    enterFace = switch (axis) {
                        case 0 -> delta > 0 ? BlockFace.WEST : BlockFace.EAST;
                        case 1 -> delta > 0 ? BlockFace.BOTTOM : BlockFace.TOP;
                        default -> delta > 0 ? BlockFace.NORTH : BlockFace.SOUTH;
                    };
                }
                exit = Math.min(exit, tFar);
                if (enter > exit) miss = true;
            }
            if (miss || enterFace == null || enter >= closestT) continue;
            closestT = enter;
            closest = new Hit(from.add(diff.mul(enter)), enterFace);
        }
        return closest;
    }

    private static boolean containsPoint(BoundingBox box, double x, double y, double z) {
        return x >= box.minX() && x < box.maxX() && y >= box.minY() && y < box.maxY() && z >= box.minZ() && z < box.maxZ();
    }

    private static double[] edges(List<BoundingBox> boxes, double min, double max,
                                  ToDoubleFunction<BoundingBox> boxMin, ToDoubleFunction<BoundingBox> boxMax) {
        var edges = new double[boxes.size() * 2 + 2];
        int count = 0;
        edges[count++] = min;
        edges[count++] = max;
        for (var box : boxes) {
            double lo = boxMin.applyAsDouble(box), hi = boxMax.applyAsDouble(box);
            if (lo > min && lo < max) edges[count++] = lo;
            if (hi > min && hi < max) edges[count++] = hi;
        }
        var result = Arrays.copyOf(edges, count);
        Arrays.sort(result);
        return Arrays.stream(result).distinct().toArray();
    }

    private static BlockFace nearestFace(Vec direction) {
        double ax = Math.abs(direction.x()), ay = Math.abs(direction.y()), az = Math.abs(direction.z());
        if (ax >= ay && ax >= az) return direction.x() > 0 ? BlockFace.EAST : BlockFace.WEST;
        if (ay >= az) return direction.y() > 0 ? BlockFace.TOP : BlockFace.BOTTOM;
        return direction.z() > 0 ? BlockFace.SOUTH : BlockFace.NORTH;
    }

    private Shapes() {
    }
}
