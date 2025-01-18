package net.hollowcube.mapmaker.dev.element;

import net.hollowcube.mapmaker.dev.render.RenderContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RowNode implements Node {
    private final List<Node> children;

    public RowNode(@NotNull List<Node> children) {
        this.children = children;
    }

    @Override public void render(@NotNull RenderContext context) {
        
    }
}
