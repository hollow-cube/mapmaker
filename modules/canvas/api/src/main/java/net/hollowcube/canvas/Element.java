package net.hollowcube.canvas;

import net.hollowcube.canvas.util.ElementLike;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Lowest form of a serverside UI element.
 */
public interface Element extends ElementLike {

    @Nullable String id();

    void setLoading(boolean loading);

    @Override
    default @NotNull Element element() {
        return this;
    }
}
