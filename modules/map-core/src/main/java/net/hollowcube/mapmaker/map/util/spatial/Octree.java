package net.hollowcube.mapmaker.map.util.spatial;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.List;

@NotNullByDefault
public interface Octree {

    /// Creates a new [Octree] with a bounding box that covers 2^scalePowerOfTwo blocks in all
    /// dimensions.
    ///
    /// @param scalePowerOfTwo The power of two for the scale of the octree. For example, 2 means 4x4x4
    static Octree simpleOctree(int scalePowerOfTwo, List<SpatialObject> objects) {
        return SimpleOctreeNode.create(scalePowerOfTwo, objects);
    }

    List<SpatialObject> intersectingObjects(BoundingBox boundingBox);

    record BoundingBoxDebug(boolean isObject, BoundingBox boundingBox) {}

    List<BoundingBoxDebug> debugBoundingBoxes();

}
