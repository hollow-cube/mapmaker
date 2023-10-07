package net.hollowcube.canvas.internal;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.ViewElement;
import org.jetbrains.annotations.NotNull;

public interface ViewProvider {

    <T extends View> @NotNull ViewElement viewFor(
            @NotNull Context context,
            @NotNull Class<? extends T> viewClass, @NotNull T view,
            @NotNull Runnable mount, @NotNull Runnable unmount
    );

}
