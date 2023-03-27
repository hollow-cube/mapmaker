package net.hollowcube.canvas.internal.standalone;

import net.hollowcube.canvas.internal.standalone.context.ElementContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class ContainerElement extends BaseElement {

    private List<BaseElement> children = new ArrayList<>();

    protected ContainerElement(@NotNull ElementContext context, @Nullable String id, int width, int height) {
        super(context, id, width, height);
    }

    protected ContainerElement(@NotNull ElementContext context, @NotNull ContainerElement other) {
        super(context, other);
        for (var child : other.children) {
            children.add(child.clone(context));
        }
    }

    @Override
    public void buildTitle(@NotNull StringBuilder sb) {
        super.buildTitle(sb);
        for (var child : children) {
            child.buildTitle(sb);
        }
    }

    @Override
    public void performSignal(@NotNull String name, @NotNull Object... args) {
        for (var child : children) {
            child.performSignal(name, args);
        }
    }

    @Override
    public @Nullable BaseElement findById(@NotNull String id) {
        var found = super.findById(id);
        if (found != null) return found;

        for (var child : children) {
            // If the child is a view, we only check that ID.
            // This is to prevent searching inner views for IDs (aka implement id scoping).
            if (child instanceof ViewContainer) {
                if (id.equals(child.id()))
                    return child;
                continue;
            }

            // Otherwise, search the child recursively.
            found = child.findById(id);
            if (found != null) return found;
        }

        return null;
    }

    public @NotNull List<@NotNull BaseElement> children() {
        return List.copyOf(children);
    }

    public void addChild(@NotNull BaseElement child) {
        children.add(child);
    }

    @Override
    public @NotNull ContainerElement clone(@NotNull ElementContext context) {
        throw new UnsupportedOperationException("Clone not implemented for ContainerElement");
    }
}
