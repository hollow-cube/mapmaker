package net.hollowcube.canvas.view;

import org.jetbrains.annotations.NotNull;

import static org.junit.jupiter.api.Assertions.*;

public class MockViewContext extends ViewContextImpl {

    private int redrawCount = 0;

    public MockViewContext() {
        super(null);
    }

    public void safeRender(@NotNull ViewFunc func) {
        beginRender();
        func.construct(this);
        endRender();
    }

    @Override
    public void redraw() {
        redrawCount++;
    }

    public void assertChildPresent(@NotNull String name) {
        assertTrue(children.containsKey(name));
    }

    public void assertChildNotPresent(@NotNull String name) {
        assertFalse(children.containsKey(name));
    }

    public void assertRedrawCount(int count) {
        assertEquals(count, redrawCount);
    }
}
