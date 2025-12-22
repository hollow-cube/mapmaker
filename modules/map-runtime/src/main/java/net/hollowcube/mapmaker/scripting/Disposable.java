package net.hollowcube.mapmaker.scripting;

import org.jetbrains.annotations.Nullable;

public interface Disposable {

    void dispose();

    boolean isDisposed();

    /// The chunkName which created this Disposable. Used to selectively invalidate on dispose.
    /// If null and [#disposeOnReload()], it will not dispose.
    default @Nullable String chunkName() {
        return null;
    }

    /// If true, this will dispose the object when a reload occurs.
    default boolean disposeOnReload() {
        return false;
    }

}
