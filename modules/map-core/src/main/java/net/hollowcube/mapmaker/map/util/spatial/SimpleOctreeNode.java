package net.hollowcube.mapmaker.map.util.spatial;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;

@NotNullByDefault
final class SimpleOctreeNode implements Octree {
    // Don't make a region smaller than a single block
    private static final int MIN_REGION_SIZE = 1;

    static final byte FLAG_BUILT = 1;
    static final byte FLAG_NEEDS_REBUILD = 2;

    static SimpleOctreeNode create(int scalePowerOfTwo, SpatialObject... objects) {
        return create(scalePowerOfTwo, List.of(objects));
    }

    static SimpleOctreeNode create(int scalePowerOfTwo, List<SpatialObject> objects) {
        float scale = (float) Math.pow(2, scalePowerOfTwo) / 2;
        var rootBoundingBox = new BoundingBox(-scale, -scale, -scale, scale, scale, scale);
        return new SimpleOctreeNode(null, rootBoundingBox, new ArrayList<>(objects));
    }

    private final @Nullable Octree parent;
    private final BoundingBox boundingBox;

    @VisibleForTesting
    @Nullable List<SpatialObject> objects;
    @VisibleForTesting
    SimpleOctreeNode @Nullable [] children;
    @VisibleForTesting
    byte activeChildren = 0;

    @VisibleForTesting
    byte flags;

    public SimpleOctreeNode(
            @Nullable Octree parent,
            BoundingBox boundingBox,
            List<SpatialObject> objects
    ) {
        this.parent = parent;
        this.boundingBox = boundingBox;
        this.objects = objects;
        this.children = null;
    }

    @Override
    public List<SpatialObject> intersectingObjects(BoundingBox boundingBox) {
        var result = new ArrayList<SpatialObject>();
        collectIntersectingObjects(boundingBox, result);
        return result;
    }

    private void collectIntersectingObjects(BoundingBox testBoundingBox, List<SpatialObject> result) {
        if ((this.flags & FLAG_BUILT) == 0) buildInitial();

        // Check against all our local objects
        if (objects != null && !objects.isEmpty()) {
            for (SpatialObject object : objects) {
                if (testBoundingBox.intersects(object.boundingBox()))
                    result.add(object);
            }
        }

        // If we have children, check them too
        if (children != null && activeChildren != 0) {
            for (int i = 0; i < 8; i++) {
                if ((activeChildren & (1 << i)) == 0) continue;

                var child = children[i];
                if (child.boundingBox.intersects(testBoundingBox))
                    child.collectIntersectingObjects(testBoundingBox, result);
            }
        }
    }

    private void buildInitial() {
        this.flags |= FLAG_BUILT; // Mark as built
        if (objects == null || objects.size() <= 1)
            return; // No need to go deeper we are at a leaf

        var size = boundingBox.size();
        if (size.x() <= MIN_REGION_SIZE && size.y() <= MIN_REGION_SIZE && size.z() <= MIN_REGION_SIZE)
            return; // Not allowed to go deeper.

        // Create the children
        float centerX = (boundingBox.minX() + boundingBox.maxX()) / 2;
        float centerY = (boundingBox.minY() + boundingBox.maxY()) / 2;
        float centerZ = (boundingBox.minZ() + boundingBox.maxZ()) / 2;
        var octants = new BoundingBox[]{
                new BoundingBox(boundingBox.minX(), boundingBox.minY(), boundingBox.minZ(), centerX, centerY, centerZ),
                new BoundingBox(centerX, boundingBox.minY(), boundingBox.minZ(), boundingBox.maxX(), centerY, centerZ),
                new BoundingBox(centerX, boundingBox.minY(), centerZ, boundingBox.maxX(), centerY, boundingBox.maxZ()),
                new BoundingBox(boundingBox.minX(), boundingBox.minY(), centerZ, centerX, centerY, boundingBox.maxZ()),
                new BoundingBox(boundingBox.minX(), centerY, boundingBox.minZ(), centerX, boundingBox.maxY(), centerZ),
                new BoundingBox(centerX, centerY, boundingBox.minZ(), boundingBox.maxX(), boundingBox.maxY(), centerZ),
                new BoundingBox(centerX, centerY, centerZ, boundingBox.maxX(), boundingBox.maxY(), boundingBox.maxZ()),
                new BoundingBox(boundingBox.minX(), centerY, centerZ, centerX, boundingBox.maxY(), boundingBox.maxZ()),
        };
        //noinspection unchecked
        List<SpatialObject>[] childObjects = new ArrayList[]{
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()
        };

        // Try to fit objects into children
        var iter = this.objects.iterator();
        while (iter.hasNext()) {
            var object = iter.next();
            for (int i = 0; i < octants.length; i++) {
                if (octants[i].contains(object.boundingBox())) {
                    childObjects[i].add(object);
                    iter.remove(); // Remove from parent
                    break; // No need to check other octants
                }
            }
        }

        // Create the actual children where we have objects
        children = new SimpleOctreeNode[8];
        for (int i = 0; i < octants.length; i++) {
            if (!childObjects[i].isEmpty()) {
                children[i] = new SimpleOctreeNode(this, octants[i], childObjects[i]);
                activeChildren |= (byte) (1 << i);
            }
        }
    }

    @Override
    public List<BoundingBoxDebug> debugBoundingBoxes() {
        List<BoundingBoxDebug> result = new ArrayList<>();
        resolveDebugBoundingBoxes(result);
        return result;
    }

    private void resolveDebugBoundingBoxes(List<BoundingBoxDebug> result) {
        if ((flags & FLAG_BUILT) == 0) buildInitial();

        result.add(new BoundingBoxDebug(false, boundingBox));
        if (objects != null && !objects.isEmpty()) {
            for (SpatialObject object : objects) {
                result.add(new BoundingBoxDebug(true, object.boundingBox()));
            }
        }

        // If we have children, add their bounding boxes too
        if (children != null && activeChildren != 0) {
            for (int i = 0; i < 8; i++) {
                if ((activeChildren & (1 << i)) == 0) continue;

                children[i].resolveDebugBoundingBoxes(result);
            }
        }
    }
}
