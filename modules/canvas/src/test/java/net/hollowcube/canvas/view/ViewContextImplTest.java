package net.hollowcube.canvas.view;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

public class ViewContextImplTest {

    @Test // Ensure the parent keeps the child across renders
    public void testParentChildPreservation() {
        var root = new ViewContextImpl.Root(null);
        var child1 = new AtomicReference<ViewContext>();
        var child2 = new AtomicReference<ViewContext>();
        root.create("test", c -> {
            child1.set(c);
            return new MockView(1, 1);
        });
        root.setRedrawFunc(() -> {
            root.create("test", c -> {
                child2.set(c);
                return new MockView(1, 1);
            });
        });
        root.redraw();

        assertSame(child1.get(), child2.get());
    }


    @Test // Ensure the parent does not keep the child when id changes
    public void testParentChildPreservation2() {
        var root = new ViewContextImpl.Root(null);
        var child1 = new AtomicReference<ViewContext>();
        var child2 = new AtomicReference<ViewContext>();
        root.create("test", c -> {
            child1.set(c);
            return new MockView(1, 1);
        });
        root.setRedrawFunc(() -> {
            root.create("test2", c -> {
                child2.set(c);
                return new MockView(1, 1);
            });
        });
        root.redraw();

        assertNotSame(child1.get(), child2.get());
    }

}
