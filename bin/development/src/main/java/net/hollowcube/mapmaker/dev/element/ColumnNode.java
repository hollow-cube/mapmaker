package net.hollowcube.mapmaker.dev.element;

import net.hollowcube.mapmaker.dev.render.RenderContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ColumnNode implements Node {
    private final List<Node> children;

    public ColumnNode(@NotNull List<Node> children) {
        this.children = children;
    }

    @Override
    public void render(@NotNull RenderContext context) {
        for (Node child : children) {
            child.render(context);
        }
    }

    @Override
    public String toString() {
        return "ColumnNode{" +
                "children=" + children +
                '}';
    }
}
