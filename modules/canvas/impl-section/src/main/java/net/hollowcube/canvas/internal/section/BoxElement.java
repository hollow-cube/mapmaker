package net.hollowcube.canvas.internal.section;

import net.hollowcube.canvas.internal.section.trait.DepthAware;
import net.hollowcube.canvas.internal.section.trait.SpriteHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

//todo make me package private again
public class BoxElement extends BaseParentWithChildrenElement implements DepthAware, SpriteHolder {

    public enum Align {
        LTR,
        TTB,
    }

    protected final Align align;

    public BoxElement(@Nullable String id, int width, int height, @NotNull Align align) {
        super(id, width, height);
        this.align = align;
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
