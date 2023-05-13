package net.hollowcube.canvas;

import net.hollowcube.canvas.internal.Context;
import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

public abstract class View implements Element {
    private static final ExecutorService VIRTUAL_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private final Context context;
    private final Element delegate;

    protected View(@NotNull Context context) {
        this.context = context;

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

    // Slot/Signal

    @Override
    public void performSignal(@NotNull String name, @NotNull Object... args) {
        context.performSignal(name, args);
    }

    // Routing

    public void pushView(@NotNull Function<Context, View> viewProvider) {
        context.pushView(viewProvider.apply(context));
    }

    public void pushView(@NotNull View view) {
        context.pushView(view);
    }

    public void popView() {
        context.popView();
    }


    /**
     * Called when this view is mounted (shown to a player).
     * <p>
     * Can be used to refresh state, subscribe to an event, etc.
     */
    protected void mount() {
    }

    /**
     * Called when this view is unmounted (stopped being shown to a player).
     * <p>
     * Can be used to unsubscribe from an event, etc.
     */
    protected void unmount() {
    }

    protected void async(@NotNull Runnable func) {
        VIRTUAL_EXECUTOR.submit(() -> {
            try {
                func.run();
            } catch (Exception e) {
                MinecraftServer.getExceptionManager().handleException(e);
            }
        });
    }

    protected <T> @NotNull Future<T> async(@NotNull Callable<T> func) {
        return VIRTUAL_EXECUTOR.submit(func);
    }
}
