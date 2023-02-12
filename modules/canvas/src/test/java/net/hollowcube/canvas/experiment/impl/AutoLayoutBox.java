package net.hollowcube.canvas.experiment.impl;

import net.hollowcube.canvas.ParentSection;
import net.hollowcube.canvas.Section;
import net.hollowcube.canvas.SectionLike;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AutoLayoutBox extends ParentSection implements Element {

    public enum Align {
        LTR,
//        RTL,
        TTB,
//        BTT,
    }

    private final String id;
    private final Align align;
    private final List<Section> children = new ArrayList<>();

    public AutoLayoutBox(@Nullable String id, int width, int height, Align align) {
        super(width, height);
        this.id = id;
        this.align = align;
    }

    @Override
    public @Nullable String id() {
        return id;
    }

    @Override
    public @Nullable Element findById(@NotNull String id) {
        if (id.equals(id())) return this;
        for (Section child : children) {
            if (child instanceof Element element) {
                Element found = element.findById(id);
                if (found != null) return found;
            }
        }
        return null;
    }

    public void addChild(@NotNull SectionLike child) {
        children.add(child.section());
    }

    @Override
    protected void mount() {
        super.mount();
        clear();

        if (align == Align.LTR) {
            int x = 0;
            for (Section child : children) {
                mountChild(x, 0, child);
                x += child.width();
            }
        } else if (align == Align.TTB) {
            int y = 0;
            for (Section child : children) {
                mountChild(0, y, child);
                y += child.height();
            }
        }
    }
}
