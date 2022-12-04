package net.hollowcube.mapmaker.hub.gui.font;

import org.jetbrains.annotations.NotNull;

public record FontKey(@NotNull String text, int width) {
    @Override
    public String toString() {
        return text;
    }
}
