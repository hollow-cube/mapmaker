package net.hollowcube.canvas;

import org.junit.jupiter.api.Test;

public class TestSection {



    /** Various assertions ensuring errors when accessing methods which require being mounted. */
    public static class TestUnmountedSanity {

        @Test
        public void testFind() {

        }

        @Test
        public void testGetIndexInParent() {

        }

        @Test
        public void testOffset() {

        }

        @Test
        public void testParent() {

        }

        @Test
        public void testUnmountedGetContext() {

        }

    }

    /** Should implement Section, but cannot because */
    private static class SectionImpl extends ItemSection {
        public SectionImpl(int width, int height) {
            super(width, height);
        }
    }

}
