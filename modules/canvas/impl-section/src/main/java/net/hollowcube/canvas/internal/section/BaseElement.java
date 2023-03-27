package net.hollowcube.canvas.internal.section;

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

    @Override
    default @NotNull State getState() {
        throw new UnsupportedOperationException("impl-section does not support new state system");
    }

    @Override
    default void setState(@NotNull State state) {
        throw new UnsupportedOperationException("impl-section does not support new state system");
    }

    default void wireAction(@NotNull View view, @NotNull Method method) {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " does not support actions.");
    }

    default void performSignal(@NotNull String name, @NotNull Object... args) {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " does not support signals.");
    }

}
