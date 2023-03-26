package net.hollowcube.canvas.internal.standalone.context;

public interface ElementContext {

    /**
     * Signals that this context has been modified and should be updated.
     */
    void markDirty();
}
