package net.hollowcube.mapmaker.map.util.spatial;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundingBoxTest {

    @Test
    void containsPointInside() {
        BoundingBox box = new BoundingBox(0, 0, 0, 10, 10, 10);
        assertTrue(box.contains(5, 5, 5));
    }

    @Test
    void containsPointOnEdge() {
        BoundingBox box = new BoundingBox(0, 0, 0, 10, 10, 10);
        assertTrue(box.contains(10, 5, 5)); // On the edge
        assertTrue(box.contains(0, 5, 5)); // On the edge
        assertTrue(box.contains(5, 10, 5)); // On the edge
        assertTrue(box.contains(5, 0, 5)); // On the edge
        assertTrue(box.contains(5, 5, 10)); // On the edge
        assertTrue(box.contains(5, 5, 0)); // On the edge
        assertTrue(box.contains(0, 0, 0)); // Corner
    }

    @Test
    void doesNotContainPointOutside() {
        BoundingBox box = new BoundingBox(0, 0, 0, 10, 10, 10);
        assertFalse(box.contains(-1, 5, 5)); // Outside
        assertFalse(box.contains(11, 5, 5)); // Outside
        assertFalse(box.contains(5, -1, 5)); // Outside
        assertFalse(box.contains(5, 11, 5)); // Outside
        assertFalse(box.contains(5, 5, -1)); // Outside
        assertFalse(box.contains(5, 5, 11)); // Outside
    }

    @Test
    void containsAnotherBoxInside() {
        BoundingBox box1 = new BoundingBox(0, 0, 0, 10, 10, 10);
        BoundingBox box2 = new BoundingBox(1, 1, 1, 9, 9, 9);
        assertTrue(box1.contains(box2));
    }

    @Test
    void doesNotContainAnotherBoxOutside() {
        BoundingBox box1 = new BoundingBox(0, 0, 0, 10, 10, 10);
        BoundingBox box2 = new BoundingBox(-1, -1, -1, 5, 5, 5);
        assertFalse(box1.contains(box2));
    }

    @Test
    void doesNotContainAnotherBoxPartiallyInside() {
        BoundingBox box1 = new BoundingBox(0, 0, 0, 10, 10, 10);
        BoundingBox box2 = new BoundingBox(5, 5, 5, 15, 15, 15);
        assertFalse(box1.contains(box2));
    }

    @Test
    void intersectsWithAnotherBox() {
        BoundingBox box1 = new BoundingBox(0, 0, 0, 10, 10, 10);
        BoundingBox box2 = new BoundingBox(5, 5, 5, 15, 15, 15);
        assertTrue(box1.intersects(box2));
    }

    @Test
    void doesNotIntersectWithNonOverlappingBox() {
        BoundingBox box1 = new BoundingBox(0, 0, 0, 10, 10, 10);
        BoundingBox box2 = new BoundingBox(11, 11, 11, 15, 15, 15);
        assertFalse(box1.intersects(box2));
    }

    @Test
    void intersectsWithTouchingBox() {
        BoundingBox box1 = new BoundingBox(0, 0, 0, 10, 10, 10);
        BoundingBox box2 = new BoundingBox(10, 5, 5, 15, 15, 15); // Touching at the edge
        assertTrue(box1.intersects(box2));
    }

    @Test
    void doesNotIntersectWithSeparatedBox() {
        BoundingBox box1 = new BoundingBox(0, 0, 0, 10, 10, 10);
        BoundingBox box2 = new BoundingBox(20, 20, 20, 30, 30, 30); // Completely separated
        assertFalse(box1.intersects(box2));
    }

    @Test
    void intersectsWithBoxOnEdge() {
        BoundingBox box1 = new BoundingBox(0, 0, 0, 10, 10, 10);
        BoundingBox box2 = new BoundingBox(10, 0, 0, 20, 10, 10); // Touching at the edge
        assertTrue(box1.intersects(box2));
    }

    @Test
    void intersectsWithSelf() {
        BoundingBox box = new BoundingBox(0, 0, 0, 10, 10, 10);
        assertTrue(box.intersects(box)); // A box should always intersect with itself
    }
}
