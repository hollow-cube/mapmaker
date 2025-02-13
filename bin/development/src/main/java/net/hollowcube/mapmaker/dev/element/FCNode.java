package net.hollowcube.mapmaker.dev.element;

import net.hollowcube.mapmaker.dev.render.RenderContext;
import org.jetbrains.annotations.NotNull;

public class FCNode extends Node {
    private final Node inner;

    public FCNode(Node inner) {
        this.inner = inner;
    }

    @Override
    public void render(@NotNull RenderContext context) {
        inner.render(context);
    }

    @Override
    public String toString() {
        return "FCNode{" +
                "inner=" + inner +
                '}';
    }
}
