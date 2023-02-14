package net.hollowcube.mapmaker.hub.guiold.font;

import org.jetbrains.annotations.NotNull;

public record FontKey(@NotNull String text, int width) {
    @Override
    public String toString() {
        return text;
    }
}
