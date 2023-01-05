package net.hollowcube.canvas.view;

import net.hollowcube.canvas.view.util.TestView;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class ViewContextImplTest {

    @Test // Ensure the parent keeps the child across renders
    public void testParentChildPreservation() {
        var root = new ViewContextImpl.Root();
        var child1 = new AtomicReference<ViewContext>();
        var child2 = new AtomicReference<ViewContext>();
        root.create("test", c -> {
            child1.set(c);
            return new TestView(1, 1);
        });
        root.setRedrawFunc(() -> {
            root.create("test", c -> {
                child2.set(c);
                return new TestView(1, 1);
            });
        });
        root.redraw();

        assertSame(child1.get(), child2.get());
    }


    @Test // Ensure the parent does not keep the child when id changes
    public void testParentChildPreservation2() {
        var root = new ViewContextImpl.Root();
        var child1 = new AtomicReference<ViewContext>();
        var child2 = new AtomicReference<ViewContext>();
        root.create("test", c -> {
            child1.set(c);
            return new TestView(1, 1);
        });
        root.setRedrawFunc(() -> {
            root.create("test2", c -> {
                child2.set(c);
                return new TestView(1, 1);
            });
        });
        root.redraw();

        assertNotSame(child1.get(), child2.get());
    }

    @Test
    public void testFlag() {
        var context = new ViewContextImpl(null);
        assertEquals(0, context.flag());
        assertEquals(1, context.flag(1));
        assertEquals(1, context.flag());
        assertEquals(2, context.flag(2));
    }

}
