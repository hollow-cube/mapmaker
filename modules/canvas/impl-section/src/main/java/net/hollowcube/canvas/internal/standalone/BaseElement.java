package net.hollowcube.canvas.internal.standalone;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.section.SectionLike;
import net.hollowcube.canvas.util.HistoryStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

public interface BaseElement extends Element, SectionLike  {

    default @Nullable Element findById(@NotNull String id) {
        if (id.equals(id())) return this;
        return null;
    }

    void setAssociatedView(@NotNull View view, @Nullable Runnable mount);

    @Nullable View getAssociatedView();

    @NotNull HistoryStack history();

    BaseElement clone();

    default void wireAction(@NotNull View view, @NotNull Method method) {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " does not support actions.");
    }

}
