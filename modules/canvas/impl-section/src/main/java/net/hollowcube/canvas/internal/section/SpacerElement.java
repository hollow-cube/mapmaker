package net.hollowcube.canvas.internal.section;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.section.ParentSection;
import net.hollowcube.canvas.util.HistoryStack;
import org.jetbrains.annotations.NotNull;
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
    public @NotNull HistoryStack history() {
        throw new UnsupportedOperationException("Spacer does not have a history.");
    }

    @Override
    public void setAssociatedView(@NotNull View view, @Nullable Runnable mount) {
        throw new UnsupportedOperationException("Spacer does not have an associated view.");
    }

    @Override
    public @Nullable View getAssociatedView() {
        return null;
    }

    @Override
    public BaseElement clone() {
        return new SpacerElement(width(), height());
    }
}
