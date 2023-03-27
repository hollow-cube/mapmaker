package net.hollowcube.canvas.internal;

import org.jetbrains.annotations.NotNull;

/**
 * An implementation dependent, opaque, context object.
 * <p>
 * Used to pass context to components.
 */
public interface Context {

    @NotNull ViewProvider viewProvider();

    void performSignal(@NotNull String name, @NotNull Object... args);

}
