package net.hollowcube.mapmaker.hub.gui.common;

import net.hollowcube.canvas.ParentSection;
import net.hollowcube.canvas.Section;
import org.jetbrains.annotations.NotNull;

public class ViewBuilder extends ParentSection {
    private final String id;

    public ViewBuilder(int width, int height, @NotNull String id) {
        super(width, height);
        this.id = id;
    }

    public @NotNull ViewBuilder backOrClose(int x, int y) {
        add(x, y, new BackOrCloseButton());
        return this;
    }

    public @NotNull ViewBuilder info(int x, int y) {
        add(x, y, new InfoButton("gui." + id + ".info"));
        return this;
    }

    public @NotNull ViewBuilder set(int x, int y, @NotNull Section section) {
        add(x, y, section);
        return this;
    }
}
