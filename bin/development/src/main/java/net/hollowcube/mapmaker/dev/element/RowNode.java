package net.hollowcube.mapmaker.dev.element;

import net.hollowcube.mapmaker.dev.render.RenderContext;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.hollowcube.mapmaker.dev.element.PropHelper.childrenAsList;

public class RowNode extends Node {
    private List<Node> children;

    @Override
    public @NotNull RowNode readProps(@NotNull Value props, @NotNull Value[] children) {
        super.readProps(props, children);

        this.children = childrenAsList(children);

        return this;
    }

    @Override
    public void render(@NotNull RenderContext context) {
        for (var child : children) {
            child.render(context);
        }
    }
}
