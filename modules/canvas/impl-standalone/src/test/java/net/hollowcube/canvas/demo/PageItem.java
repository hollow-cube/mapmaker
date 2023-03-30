package net.hollowcube.canvas.demo;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public class PageItem extends View {

    private @Outlet("label") Label label;

    public PageItem(@NotNull Context context) {
        super(context);
//        label.setArgs(Component.text(name));
    }

    public void setName(String name) {
        label.setArgs(Component.text(name));
    }

}
