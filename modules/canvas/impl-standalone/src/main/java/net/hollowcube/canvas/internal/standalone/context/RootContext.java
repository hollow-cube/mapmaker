package net.hollowcube.canvas.internal.standalone.context;

import net.hollowcube.canvas.internal.Context;
import net.hollowcube.canvas.internal.ViewProvider;
import org.jetbrains.annotations.NotNull;

public record RootContext(
        @NotNull ViewProvider viewProvider
) implements Context {

}
