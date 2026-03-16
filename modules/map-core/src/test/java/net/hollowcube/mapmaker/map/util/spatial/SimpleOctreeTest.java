package net.hollowcube.mapmaker.map.util.spatial;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimpleOctreeTest {

    @Test
    void testCreateSingleObject() {
        var obj1 = new SimpleSpatialObject(0, 0, 0, 10, 10, 10);
        var octree = SimpleOctreeNode.create(8, obj1);

        var contains = octree.intersectingObjects(new BoundingBox(-1, -1, -1, 11, 11, 11));
        assertEquals(1, contains.size());
        assertEquals(obj1, contains.getFirst());

        var overlap = octree.intersectingObjects(new BoundingBox(5, 5, 5, 15, 15, 15));
        assertEquals(1, overlap.size());
        assertEquals(obj1, overlap.getFirst());

        var outside = octree.intersectingObjects(new BoundingBox(20, 20, 20, 30, 30, 30));
        assertEquals(0, outside.size());

        // Would expect no children
        assertEquals(0, octree.activeChildren);
    }

    @Test
    void testMultiObjectDifferentOctantSingleLayer() {
        var obj1 = new SimpleSpatialObject(5, 5, 5, 10, 10, 10);
        var obj2 = new SimpleSpatialObject(-5, 5, 5, -10, 10, 10);
        var octree = SimpleOctreeNode.create(8, obj1, obj2);

        var containsAll = octree.intersectingObjects(new BoundingBox(-15, -15, -15, 15, 15, 15));
        assertEquals(2, containsAll.size());
        assertEquals(obj1, containsAll.get(0));
        assertEquals(obj2, containsAll.get(1));

        var containsObj1 = octree.intersectingObjects(new BoundingBox(0, 0, 0, 15, 15, 15));
        assertEquals(1, containsObj1.size());
        assertEquals(obj1, containsObj1.getFirst());

        var containsObj2 = octree.intersectingObjects(new BoundingBox(-15, 0, 0, 0, 15, 15));
        assertEquals(1, containsObj2.size());
        assertEquals(obj2, containsObj2.getFirst());

        var outside = octree.intersectingObjects(new BoundingBox(20, 20, 20, 30, 30, 30));
        assertEquals(0, outside.size());

        // Would expect 2 children no objects, and should be built
        assertTrue((octree.flags & SimpleOctreeNode.FLAG_BUILT) != 0);
        assertEquals((byte) 0b11000000, octree.activeChildren);
        assertTrue(octree.objects == null || octree.objects.isEmpty());
    }

    @Test
    void testUninitializedChild() {
        // Children which are never visited should never be initialized.
        var obj1 = new SimpleSpatialObject(5, 5, 5, 10, 10, 10);
        var obj2 = new SimpleSpatialObject(-5, 5, 5, -10, 10, 10);
        var octree = SimpleOctreeNode.create(8, obj1, obj2);

        // Only search the octant with obj1, so the obj2 child should _not_ be built.
        assertEquals(1, octree.intersectingObjects(new BoundingBox(1, 1, 1, 15, 15, 15)).size());
        assertEquals((byte) 0b11000000, octree.activeChildren);
        assertNotEquals(0, octree.children[6].flags & SimpleOctreeNode.FLAG_BUILT);
        assertEquals(0, octree.children[7].flags & SimpleOctreeNode.FLAG_BUILT);
    }

    @Test
    void testOverlappingObjects() {
        var obj1 = new SimpleSpatialObject(0, 0, 0, 10, 10, 10);
        var obj2 = new SimpleSpatialObject(5, 5, 5, 15, 15, 15);
        var octree = SimpleOctreeNode.create(8, obj1, obj2);

        var overlap = octree.intersectingObjects(new BoundingBox(-1, -1, -1, 20, 20, 20));
        assertEquals(2, overlap.size());
        assertTrue(overlap.contains(obj1));
        assertTrue(overlap.contains(obj2));

        // TODO: This test is broken. I know nothing about what this test does or how it works. Matt pls fix.
        assertEquals(1, octree.objects.size()); // obj1 must still be in the root
    }

}
