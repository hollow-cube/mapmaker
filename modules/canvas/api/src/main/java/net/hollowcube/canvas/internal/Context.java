package net.hollowcube.canvas.internal;

import net.hollowcube.canvas.View;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * An implementation dependent, opaque, context object.
 * <p>
 * Used to pass context to components.
 */
public interface Context {

    @NotNull Context with(@NotNull Map<String, Object> contextObjects);

    @NotNull ViewProvider viewProvider();

    void performSignal(@NotNull String name, @NotNull Object... args);

    void pushView(@NotNull View view);

    void popView();

}
