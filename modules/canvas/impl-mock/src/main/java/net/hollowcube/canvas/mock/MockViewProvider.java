package net.hollowcube.canvas.mock;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.canvas.internal.ViewProvider;
import org.jetbrains.annotations.NotNull;

class MockViewProvider implements ViewProvider {
    @Override
    public @NotNull <T extends View> Element viewFor(@NotNull Context context, @NotNull Class<? extends T> viewClass, @NotNull T view, @NotNull Runnable mount, @NotNull Runnable unmount) {
        return null;
    }
}
