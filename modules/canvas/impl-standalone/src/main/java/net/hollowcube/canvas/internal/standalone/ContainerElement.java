package net.hollowcube.canvas.internal.standalone;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class ContainerElement extends BaseElement {

    private List<BaseElement> children = new ArrayList<>();

    protected ContainerElement(@Nullable String id, int width, int height) {
        super(id, width, height);
    }

    protected ContainerElement(@NotNull ContainerElement other) {
        super(other);
        for (var child : other.children) {
            children.add(child.dup());
        }
    }

    public @NotNull List<@NotNull BaseElement> children() {
        return List.copyOf(children);
    }

    public void addChild(@NotNull BaseElement child) {
        children.add(child);
    }

    @Override
    public @NotNull ContainerElement dup() {
        throw new UnsupportedOperationException("Clone not implemented for ContainerElement");
    }
}
