package net.hollowcube.canvas.internal.standalone;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.internal.standalone.trait.DepthAware;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BoxElement extends BaseParentElement implements DepthAware {

    public enum Align {
        LTR,
        TTB,
    }

    private final Align align;

    private final List<BaseElement> children = new ArrayList<>();

    public BoxElement(@Nullable String id, int width, int height, @NotNull Align align) {
        super(id, width, height);
        this.align = align;
    }

    @Override
    public @Nullable Element findById(@NotNull String id) {
        if (id.equals(id())) return this;
        for (var child : children) {
            var found = child.findById(id);
            if (found != null) return found;
        }
        return null;
    }

    public void addChild(@NotNull BaseElement child) {
        children.add(child);
    }

    @Override
    protected void mount() {
        super.mount();

        // Clear old entries & perform layout
        clear();
        if (align == Align.LTR) {
            int x = 0;
            for (var child : children) {
                var section = child.section();
                mountChild(x, 0, section);
                x += section.width();
            }
        } else if (align == Align.TTB) {
            int y = 0;
            for (var child : children) {
                var section = child.section();
                mountChild(0, y, section);
                y += section.height();
            }
        }
    }

    @Override
    public BaseElement clone() {
        var box = new BoxElement(id(), width(), height(), align);
        for (var child : children) {
            box.children.add(child.clone());
        }
        return box;
    }
}
