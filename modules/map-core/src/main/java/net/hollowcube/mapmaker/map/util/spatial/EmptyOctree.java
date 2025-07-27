package net.hollowcube.mapmaker.map.util.spatial;

import java.util.List;

final class EmptyOctree implements Octree {
    public static final EmptyOctree INSTANCE = new EmptyOctree();

    @Override
    public List<SpatialObject> intersectingObjects(BoundingBox boundingBox) {
        return List.of();
    }

    @Override
    public List<BoundingBoxDebug> debugBoundingBoxes() {
        return List.of();
    }
}
