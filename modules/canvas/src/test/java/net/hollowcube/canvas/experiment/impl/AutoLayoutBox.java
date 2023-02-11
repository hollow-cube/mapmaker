package net.hollowcube.canvas.experiment.impl;

import net.hollowcube.canvas.ParentSection;
import net.hollowcube.canvas.Section;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AutoLayoutBox extends ParentSection implements Element {

    public enum Align {
        LTR,
    }

    private final String id;
    private final List<Section> children = new ArrayList<>();

    public AutoLayoutBox(@Nullable String id, int width, int height) {
        super(width, height);
        this.id = id;
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

    public void addChild(@NotNull Section child) {
        children.add(child);
    }

    @Override
    protected void mount() {
        super.mount();
        clear();

        int x = 0;
        for (Section child : children) {
            mountChild(x, 0, child);
            x += child.width();
        }
    }
}
