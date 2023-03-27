package net.hollowcube.canvas.internal.section.internal;

import net.hollowcube.canvas.internal.Context;
import net.hollowcube.canvas.internal.ViewProvider;
import org.jetbrains.annotations.NotNull;

public class ContextImpl implements Context {
    @Override
    public @NotNull ViewProvider viewProvider() {
        return ViewProviderImpl.INSTANCE;
    }

    @Override
    public void performSignal(@NotNull String name, @NotNull Object... args) {
        throw new UnsupportedOperationException("Cannot perform signal in section context");
    }
}
