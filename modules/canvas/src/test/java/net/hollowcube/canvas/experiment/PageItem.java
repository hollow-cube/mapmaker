package net.hollowcube.canvas.experiment;

import net.hollowcube.canvas.experiment.annotation.Outlet;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public class PageItem extends View {

    private final String name;

    private @Outlet Label label;

    public PageItem(@NotNull String name) {
        this.name = name;
    }

    @Override
    protected void mount() {
        label.setArgs(Component.text(name));
    }

}
