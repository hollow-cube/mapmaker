package net.hollowcube.mapmaker.scripting;

public interface Disposable {

    void dispose();

    boolean isDisposed();

    /// If true, this will dispose the object when a reload occurs.
    default boolean disposeOnReload() {
        return false;
    }

}
