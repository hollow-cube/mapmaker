package net.hollowcube.common.physics;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * See https://wiki.vg/Entity_metadata#Mobs_2
 */
public final class BoundingBox {
    public final static BoundingBox ZERO = new BoundingBox(0, 0, 0);

    private final double width, height, depth;
    private final Point offset;
    private Point relativeEnd;

    public BoundingBox(double width, double height, double depth, Point offset) {
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.offset = offset;
    }

    public BoundingBox(double width, double height, double depth) {
        this(width, height, depth, new Vec(-width / 2, 0, -depth / 2));
    }

    public boolean intersectBox(@NotNull Point positionRelative, @NotNull BoundingBox boundingBox) {
        return (minX() + positionRelative.x() <= boundingBox.maxX() - Vec.EPSILON / 2 && maxX() + positionRelative.x() >= boundingBox.minX() + Vec.EPSILON / 2) &&
                (minY() + positionRelative.y() <= boundingBox.maxY() - Vec.EPSILON / 2 && maxY() + positionRelative.y() >= boundingBox.minY() + Vec.EPSILON / 2) &&
                (minZ() + positionRelative.z() <= boundingBox.maxZ() - Vec.EPSILON / 2 && maxZ() + positionRelative.z() >= boundingBox.minZ() + Vec.EPSILON / 2);
    }

    public @NotNull Point relativeStart() {
        return offset;
    }

    public @NotNull Point relativeEnd() {
        Point relativeEnd = this.relativeEnd;
        if (relativeEnd == null) this.relativeEnd = relativeEnd = offset.add(width, height, depth);
        return relativeEnd;
    }

    @Override
    public String toString() {
        String result = "BoundingBox";
        result += "\n";
        result += "[" + minX() + " : " + maxX() + "]";
        result += "\n";
        result += "[" + minY() + " : " + maxY() + "]";
        result += "\n";
        result += "[" + minZ() + " : " + maxZ() + "]";
        return result;
    }

    /**
     * @param x the X offset
     * @param y the Y offset
     * @param z the Z offset
     * @return a new {@link BoundingBox} expanded
     */
    public @NotNull BoundingBox expand(double x, double y, double z) {
        return new BoundingBox(this.width + x, this.height + y, this.depth + z);
    }

    /**
     * @param x the X offset
     * @param y the Y offset
     * @param z the Z offset
     * @return a new bounding box contracted
     */
    public @NotNull BoundingBox contract(double x, double y, double z) {
        return new BoundingBox(this.width - x, this.height - y, this.depth - z);
    }

    public double width() {
        return width;
    }

    public double height() {
        return height;
    }

    public double depth() {
        return depth;
    }

    public double minX() {
        return relativeStart().x();
    }

    public double maxX() {
        return relativeEnd().x();
    }

    public double minY() {
        return relativeStart().y();
    }

    public double maxY() {
        return relativeEnd().y();
    }

    public double minZ() {
        return relativeStart().z();
    }

    public double maxZ() {
        return relativeEnd().z();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BoundingBox that = (BoundingBox) o;
        if (Double.compare(that.width, width) != 0) return false;
        if (Double.compare(that.height, height) != 0) return false;
        if (Double.compare(that.depth, depth) != 0) return false;
        return offset.equals(that.offset);
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height, depth, offset, relativeEnd);
    }

    public Collection<Point> getBlocks(Point point) {
        List<Point> blocks = new ArrayList<>();
        for (double x = this.minX() + point.x(); x <= this.maxX() + point.x(); x++) {
            for (double y = this.minY() + point.y(); y <= this.maxY() + point.y(); y++) {
                for (double z = this.minZ() + point.z(); z <= this.maxZ() + point.z(); z++) {
                    blocks.add(new Vec((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z)));
                }
                blocks.add(new Vec((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(this.maxX() + point.z())));
            }
            blocks.add(new Vec((int) Math.floor(x), (int) Math.floor(this.maxY() + point.y()), (int) Math.floor(this.minZ() + point.z())));
        }

        blocks.add(new Vec((int) Math.floor(this.maxX() + point.x()), (int) Math.floor(this.maxY() + point.y()), (int) Math.floor(this.maxZ() + point.z())));
        return blocks;
    }

    public boolean containsPoint(Vec start) {
        return start.x() >= minX() && start.x() <= maxX() && start.y() >= minY() && start.y() <= maxY() && start.z() >= minZ() && start.z() <= maxZ();
    }
}
