package net.hollowcube.mapmaker.map.util.spatial;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.NotNullByDefault;

// Yes i know its another bounding box
@NotNullByDefault
public record BoundingBox(
        float minX, float minY, float minZ,
        float maxX, float maxY, float maxZ
) {
    /// The zero bounding box acts as a single point at the origin.
    public static final BoundingBox ZERO = new BoundingBox(0, 0, 0, 0, 0, 0);

    public boolean contains(Point point) {
        return contains((float) point.x(), (float) point.y(), (float) point.z());
    }

    public boolean contains(float x, float y, float z) {
        return x >= minX && x <= maxX &&
                y >= minY && y <= maxY &&
                z >= minZ && z <= maxZ;
    }

    public boolean contains(BoundingBox other) {
        return this.minX <= other.minX && this.maxX >= other.maxX &&
                this.minY <= other.minY && this.maxY >= other.maxY &&
                this.minZ <= other.minZ && this.maxZ >= other.maxZ;
    }

    public boolean intersects(BoundingBox other) {
        return this.minX <= other.maxX && this.maxX >= other.minX &&
                this.minY <= other.maxY && this.maxY >= other.minY &&
                this.minZ <= other.maxZ && this.maxZ >= other.minZ;
    }

    public Vec center() {
        return new Vec(
                (minX + maxX) / 2,
                (minY + maxY) / 2,
                (minZ + maxZ) / 2
        );
    }
    
    public Vec size() {
        return new Vec(maxX - minX, maxY - minY, maxZ - minZ);
    }

}
