package net.hollowcube.canvas.experiment.impl;

import net.hollowcube.canvas.section.ParentSection;
import org.jetbrains.annotations.Nullable;

public class SpacerElement extends ParentSection implements Element {

    public SpacerElement(int width, int height) {
        super(width, height);
    }

    @Override
    public @Nullable String id() {
        return null;
    }

    @Override
    public int zIndex() {
        return 0;
    }
}
