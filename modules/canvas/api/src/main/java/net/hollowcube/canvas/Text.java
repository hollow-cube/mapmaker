package net.hollowcube.canvas;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

public interface Text extends Element {

    default void setText(@NotNull String text) {
        setText(text, NamedTextColor.WHITE);
    }

    void setText(@NotNull String text, @NotNull TextColor color);

}
