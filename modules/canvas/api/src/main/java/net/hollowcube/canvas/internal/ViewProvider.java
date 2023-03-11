package net.hollowcube.canvas.internal;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.View;
import org.jetbrains.annotations.NotNull;

public interface ViewProvider {

    <T extends View> @NotNull Element viewFor(
            @NotNull Context context,
            @NotNull Class<? extends T> viewClass, @NotNull T view,
            @NotNull Runnable mount, @NotNull Runnable unmount
    );

}
