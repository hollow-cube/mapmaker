package net.hollowcube.canvas.experiment.impl;

import net.hollowcube.canvas.internal.standalone.sprite.Sprite;
import net.hollowcube.canvas.section.ParentSection;
import net.hollowcube.canvas.section.Section;
import net.hollowcube.canvas.section.SectionLike;
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
    private int zIndex;
    private final Align align;
    private Sprite sprite = null;

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
    public int zIndex() {
        return zIndex;
    }

    public void setZIndex(int zIndex) {
        this.zIndex = zIndex;
    }

    public void setSprite(@Nullable Sprite sprite) {
        this.sprite = sprite;
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

        // Draw sprite if present
        if (sprite != null) {
            find(RootElement.class).addSprite(this, sprite, 0);
        }

        // Place entries
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

    @Override
    protected void unmount() {
        super.unmount();

        find(RootElement.class).removeSprites(this);
    }
}
