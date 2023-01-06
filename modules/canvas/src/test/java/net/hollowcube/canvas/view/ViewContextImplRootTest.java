package net.hollowcube.canvas.view;

import net.hollowcube.canvas.view.util.TestRunnable;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ViewContextImplRootTest {

    @Test
    public void testCallRedraw() {
        var runnable = new TestRunnable();
        var context = new ViewContextImpl.Root(null);
        context.setRedrawFunc(runnable);
        context.redraw();
        runnable.assertRan();
    }

    @Test
    public void testRedrawInRedraw() {
        // Re drawing during a re-draw should return _before_ redrawing to preserve the original render order.
        // More clearly, infinite loops still can happen, however another draw may not start while one is in progress.

        AtomicBoolean call1 = new AtomicBoolean(false), call2 = new AtomicBoolean(false);

        var context = new ViewContextImpl.Root(null);
        context.setRedrawFunc(() -> {
            if (!call1.get()) {
                assertFalse(call2.get());
                context.redraw();
                assertFalse(call2.get());
                call1.set(true);
            } else {
                assertTrue(call1.get());
                call2.set(true);
            }
        });
        context.redraw();

        assertTrue(call1.get());
        assertTrue(call2.get());
    }
}
