package net.hollowcube.canvas.internal;

import net.hollowcube.canvas.View;
import net.minestom.server.entity.Player;
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

    @NotNull Player player();

    void performSignal(@NotNull String name, @NotNull Object... args);

    boolean canPopView();

    void clearHistory();

    void pushView(@NotNull View view, boolean isTransient);

    void popView();

}
