package net.hollowcube.canvas.experiment.demo;

import net.hollowcube.canvas.experiment.Label;
import net.hollowcube.canvas.experiment.View;
import net.hollowcube.canvas.experiment.annotation.Outlet;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public class PageItem extends View {

    private final String name;

    private @Outlet("label") Label label;

    public PageItem(@NotNull String name) {
        this.name = name;
    }

    @Override
    protected void mount() {
        label.setArgs(Component.text(name));
    }

}
