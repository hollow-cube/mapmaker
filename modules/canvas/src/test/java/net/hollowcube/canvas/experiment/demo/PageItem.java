package net.hollowcube.canvas.experiment.demo;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Outlet;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public class PageItem extends View {

    private @Outlet("label") Label label;

    public PageItem(@NotNull String name) {
        label.setArgs(Component.text(name));
    }

}
