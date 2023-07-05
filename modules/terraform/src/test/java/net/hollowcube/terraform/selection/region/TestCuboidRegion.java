package net.hollowcube.terraform.selection.region;

import net.minestom.server.coordinate.Vec;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class TestCuboidRegion {

    @Test
    public void testSameBlock() {
        Vec pos1 = new Vec(1, 1, 1), pos2 = new Vec(1, 1, 1);
        var region = new CuboidRegion(pos1, pos2);

        assertEquals(pos1, region.min());
        assertEquals(pos1, region.max());
        assertEquals(0, region.volume());
    }

    @Test
    public void testSingleBlock() {
        Vec pos1 = new Vec(1, 1, 1), pos2 = new Vec(2, 2, 2);
        var region = new CuboidRegion(pos1, pos2);

        assertEquals(pos1, region.min());
        assertEquals(pos2, region.max());
        assertEquals(1, region.volume());
    }

    @Test
    public void testMultiBlock() {
        Vec pos1 = new Vec(1, 1, 1), pos2 = new Vec(4, 4, 4);
        var region = new CuboidRegion(pos1, pos2);

        assertEquals(pos1, region.min());
        assertEquals(pos2, region.max());
        assertEquals(27, region.volume());
    }

    @Test
    public void testMinMaxReorder() {
        Vec pos1 = new Vec(1, 5, 32), pos2 = new Vec(-1, 51, 1);
        var region = new CuboidRegion(pos1, pos2);

        assertEquals(new Vec(-1, 5, 1), region.min());
        assertEquals(new Vec(1, 51, 32), region.max());
    }

    @Nested
    class TestIterator {

        @Test
        public void testSameBlock() {
            Vec pos1 = new Vec(1, 1, 1), pos2 = new Vec(1, 1, 1);
            var region = new CuboidRegion(pos1, pos2);

            var iter = region.iterator();
            assertFalse(iter.hasNext());
            assertThrows(NoSuchElementException.class, iter::next);
        }

        @Test
        public void testSingleBlock() {
            Vec pos1 = new Vec(1, 1, 1), pos2 = new Vec(2, 2, 2);
            var region = new CuboidRegion(pos1, pos2);

            var iter = region.iterator();
            assertEquals(new Vec(1, 1, 1), iter.next());
            assertThrows(NoSuchElementException.class, iter::next);
        }

        @Test
        public void test2x2Blocks() {
            Vec pos1 = new Vec(1, 1, 1), pos2 = new Vec(3, 3, 3);
            var region = new CuboidRegion(pos1, pos2);

            var iter = region.iterator();
            assertEquals(new Vec(1, 1, 1), iter.next());
            assertEquals(new Vec(2, 1, 1), iter.next());
            assertEquals(new Vec(1, 2, 1), iter.next());
            assertEquals(new Vec(2, 2, 1), iter.next());
            assertEquals(new Vec(1, 1, 2), iter.next());
            assertEquals(new Vec(2, 1, 2), iter.next());
            assertEquals(new Vec(1, 2, 2), iter.next());
            assertEquals(new Vec(2, 2, 2), iter.next());
            assertThrows(NoSuchElementException.class, iter::next);
        }

    }
}
