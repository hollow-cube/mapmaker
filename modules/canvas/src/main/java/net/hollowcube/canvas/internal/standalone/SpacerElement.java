package net.hollowcube.canvas.internal.standalone;

import net.hollowcube.canvas.section.ParentSection;
import org.jetbrains.annotations.Nullable;

public class SpacerElement extends ParentSection implements BaseElement {

    public SpacerElement(int width, int height) {
        super(width, height);
    }

    @Override
    public @Nullable String id() {
        return null;
    }

    @Override
    public void setLoading(boolean loading) {
        throw new UnsupportedOperationException("Spacer does not have a loading state.");
    }

    @Override
    public BaseElement clone() {
        return new SpacerElement(width(), height());
    }
}
