package net.hollowcube.canvas;

import org.jetbrains.annotations.Nullable;

/**
 * Basic serverside element of a ui.
 */
public interface Element {

    @Nullable String id();

    void setLoading(boolean loading);

}
