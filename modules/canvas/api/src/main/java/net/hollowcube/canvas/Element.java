package net.hollowcube.canvas;

import net.hollowcube.canvas.util.ElementLike;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Lowest form of a serverside UI element.
 */
public interface Element extends ElementLike {
    boolean CLICK_ALLOW = true;
    boolean CLICK_DENY = false;

    enum State {
        ACTIVE,
        LOADING,
        HIDDEN,
        DISABLED
    }

    @Nullable String id();

    @Deprecated
    void setLoading(boolean loading);

    @NotNull State getState();

    void setState(@NotNull State state);

    @Override
    default @NotNull Element element() {
        return this;
    }
}
