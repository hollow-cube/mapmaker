package net.hollowcube.canvas;

import net.hollowcube.canvas.internal.Context;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

public abstract class View implements Element {
    private static final ExecutorService VIRTUAL_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private final Context context;
    private final ViewElement delegate;


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
    public final @NotNull State getState() {
        return delegate.getState();
    }

    @Override
    public final void setState(@NotNull State state) {
        delegate.setState(state);
    }

    protected @NotNull Player player() {
        return context.player();
    }

    // Slot/Signal

    @Override
    public void performSignal(@NotNull String name, @NotNull Object... args) {
        context.performSignal(name, args);
    }

    // Manual Actions

    public void addActionHandler(@NotNull String name, @NotNull Object handler) {
        delegate.addActionHandler(name, handler, false);
    }

    public void addAsyncActionHandler(@NotNull String name, @NotNull Object handler) {
        delegate.addActionHandler(name, handler, true);
    }

    // Routing

    /**
     * Returns true if there is an available view to pop, false otherwise.
     */
    public boolean canPopView() {
        return context.canPopView();
    }

    public void pushView(@NotNull Function<Context, View> viewProvider) {
        try {
            context.pushView(viewProvider.apply(context), false);
        } catch (Exception e) {
            MinecraftServer.getExceptionManager().handleException(e);
        }
    }

    public void pushView(@NotNull View view) {
        context.pushView(view, false);
    }

    public void pushTransientView(@NotNull Function<Context, View> viewProvider) {
        context.pushView(viewProvider.apply(context), true);
    }

    public void popView() {
        context.popView();
    }

    /**
     * A variant of {@link #popView()} that performs a signal on the new view after mounting it.
     */
    public void popView(@NotNull String signal, Object... args) {
        context.popView();
        context.performSignal(signal, args);
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

    public interface AsyncRunnable {
        @Async.Execute
        @Blocking
        void run() throws Exception;
    }

    @NonBlocking
    protected Future<?> async(@Async.Schedule @NotNull AsyncRunnable func) {
        return VIRTUAL_EXECUTOR.submit(() -> {
            try {
                func.run();
            } catch (Exception e) {
                MinecraftServer.getExceptionManager().handleException(e);
            }
        });
    }

    public interface AsyncCallable<T> {
        @Blocking
        T call() throws Exception;
    }

    protected <T> @NotNull Future<T> async(@NotNull AsyncCallable<T> func) {
        return VIRTUAL_EXECUTOR.submit(() -> {
            try {
                return func.call();
            } catch (Exception e) {
                MinecraftServer.getExceptionManager().handleException(e);
                return null;
            }
        });
    }
}
