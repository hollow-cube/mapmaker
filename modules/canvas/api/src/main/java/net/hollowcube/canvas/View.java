package net.hollowcube.canvas;

import net.hollowcube.canvas.internal.Context;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class View implements Element {

    private final Element delegate;

    protected View(@NotNull Context context) {
        var viewProvider = context.viewProvider();
        delegate = viewProvider.viewFor(context, getClass(), this, this::mount, this::unmount);
    }

    @Override
    public final @NotNull Element element() {
        return delegate;
    }

    @Override
    public final @Nullable String id() {
        return delegate.id();
    }

    @Override
    public final void setLoading(boolean loading) {
        delegate.setLoading(true);
    }

    @Override
    public final @NotNull State getState() {
        return delegate.getState();
    }

    @Override
    public final void setState(@NotNull State state) {
        delegate.setState(state);
    }

    /**
     * Called when this view is mounted (shown to a player).
     * <p>
     * Can be used to refresh state, subscribe to an event, etc.
     */
    protected void mount() {}

    /**
     * Called when this view is unmounted (stopped being shown to a player).
     * <p>
     * Can be used to unsubscribe from an event, etc.
     */
    protected void unmount() {}
}
