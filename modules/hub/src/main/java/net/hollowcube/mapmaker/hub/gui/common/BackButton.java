package net.hollowcube.mapmaker.hub.gui.common;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.internal.Context;
import org.jetbrains.annotations.NotNull;

public class BackButton extends View {
    public BackButton(@NotNull Context context) {
        super(context);
    }

    @Action("btn")
    public void handleClick() {
        popView();
    }
}
