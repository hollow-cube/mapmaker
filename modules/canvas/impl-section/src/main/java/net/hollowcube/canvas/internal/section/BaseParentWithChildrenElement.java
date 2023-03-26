package net.hollowcube.canvas.internal.section;

import net.hollowcube.canvas.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseParentWithChildrenElement extends BaseParentElement {

    protected final List<BaseElement> children = new ArrayList<>();

    public BaseParentWithChildrenElement(@Nullable String id, int width, int height) {
        super(id, width, height);
    }

    @Override
    public @Nullable Element findById(@NotNull String id) {
        if (id.equals(id()))
            return this;
        for (var child : children) {
            var found = child.findById(id);
            if (found != null) return found;
        }
        return null;
    }

    public void addChild(@NotNull BaseElement child) {
        children.add(child);
    }

}
