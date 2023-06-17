package net.hollowcube.canvas.internal.standalone.context;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.canvas.internal.ViewProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public record RootContext(
        @NotNull ViewProvider viewProvider
) implements Context {

    @Override
    public @NotNull Context with(@NotNull Map<String, Object> contextObjects) {
        throw new UnsupportedOperationException("Cannot create child context from root context");
    }

    @Override
    public void performSignal(@NotNull String name, @NotNull Object... args) {
        throw new UnsupportedOperationException("Cannot perform signal on root context");
    }

    @Override
    public boolean canPopView() {
        return false;
    }

    @Override
    public void pushView(@NotNull View view) {
        throw new UnsupportedOperationException("Cannot push view on root context");
    }

    @Override
    public void popView() {
        throw new UnsupportedOperationException("Cannot pop view on root context");
    }
}
