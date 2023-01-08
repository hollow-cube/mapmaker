package net.hollowcube.canvas.view.std;

import net.hollowcube.canvas.view.MockView;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class PaneViewTest {

    @Test
    public void testSizeConstraints() {
        int width = 5, height = 5;
        var view = new PaneView(width, height);
        assertEquals(width, view.width());
        assertEquals(height, view.height());
        assertEquals(width * height, view.getContents().length);
    }

    @Test
    public void testEmptySlots() {
        var view = new PaneView(5, 5);
        for (var item : view.getContents()) {
            assertTrue(item.isAir());
        }
    }

    @TestFactory
    public Stream<DynamicTest> testAdd() {
        return IntStream.range(0, 9)
                .mapToObj(i -> DynamicTest.dynamicTest("testAdd" + i, () -> {
                    var view = new PaneView(3, 3);
                    var item = new MockView(1, 1);
                    view.add(i % 3, i / 3, item);
                    assertEquals(MockView.TEST_ITEM, view.getContents()[i]);
                }));
    }

    @TestFactory
    public Stream<DynamicTest> testAddBig() {
        return IntStream.range(0, 4).mapToObj(i -> DynamicTest.dynamicTest("testAddBig" + i, () -> {
            var view = new PaneView(3, 3);
            var item = new MockView(2, 2);

            int x = i % 2, y = i / 2;
            view.add(x, y, item);

            assertEquals(MockView.TEST_ITEM, view.getContents()[x + y * 3]);
            assertEquals(MockView.TEST_ITEM, view.getContents()[x + 1 + y * 3]);
            assertEquals(MockView.TEST_ITEM, view.getContents()[x + (y + 1) * 3]);
            assertEquals(MockView.TEST_ITEM, view.getContents()[x + 1 + (y + 1) * 3]);
        }));
    }

    @Test
    public void testOutOfBounds1() {
        var view = new PaneView(3, 3);
        var item = new MockView(1, 1);
        assertThrows(IllegalArgumentException.class, () -> view.add(3, 0, item));
    }

    @Test
    public void testOutOfBounds2() {
        var view = new PaneView(3, 3);
        var item = new MockView(1, 1);
        assertThrows(IllegalArgumentException.class, () -> view.add(0, 3, item));
    }

    @Test
    public void testOutOfBounds3() {
        var view = new PaneView(3, 3);
        var item = new MockView(4, 1);
        assertThrows(IllegalArgumentException.class, () -> view.add(0, 0, item));
    }

    @Test
    public void testOutOfBounds4() {
        var view = new PaneView(3, 3);
        var item = new MockView(1, 4);
        assertThrows(IllegalArgumentException.class, () -> view.add(0, 0, item));
    }

    @Test
    public void testOverlap1() {
        var view = new PaneView(3, 3);
        var item = new MockView(1, 1);
        view.add(0, 0, item);
        assertThrows(IllegalArgumentException.class, () -> view.add(0, 0, item));
    }

    @Test
    public void testOverlap2() {
        var view = new PaneView(3, 3);
        var item = new MockView(2, 2);
        view.add(1, 1, item);
        assertThrows(IllegalArgumentException.class, () -> view.add(0, 0, item));
    }

    @Test
    public void testClickEmpty() {
        var view = new PaneView(3, 3);
        assertFalse(view.handleClick(null, 0, null));
    }

    @Test // Zero slot, no offset remapping
    public void testClickPassthrough1() {
        var view = new PaneView(3, 3);
        var item = new MockView(1, 1);
        view.add(0, 0, item);
        view.handleClick(null, 0, null);

        item.assertClicked(0);
    }

    @Test // Click a non-zero slot, still no offset remapping
    public void testClickPassthrough2() {
        var view = new PaneView(3, 3);
        var item = new MockView(3, 3);
        view.add(0, 0, item);
        view.handleClick(null, 4, null);

        item.assertClicked(4);
    }

    @Test // Click a non-zero slot, with offset remapping
    public void testClickPassthrough3() {
        var view = new PaneView(3, 3);
        var item = new MockView(2, 2);
        view.add(1, 1, item);
        view.handleClick(null, 4, null);

        item.assertClicked(0);
    }

}
