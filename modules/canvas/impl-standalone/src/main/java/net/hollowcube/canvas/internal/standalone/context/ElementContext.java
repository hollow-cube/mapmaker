package net.hollowcube.canvas.internal.standalone.context;

import net.hollowcube.canvas.internal.Context;

public interface ElementContext extends Context {

    /**
     * Signals that this context has been modified and should be updated.
     */
    @Override
    void markDirty();
}
