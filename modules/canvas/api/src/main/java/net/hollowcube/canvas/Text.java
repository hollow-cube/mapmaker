package net.hollowcube.canvas;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;

public interface Text extends Element, Label {

    /**
     * This is different from {@link #setText(String)} as it accepts a {@link Component} instead of a plain string.
     * This will automatically serialize the component to plain text including resolving translations.
     *
     * @apiNote This does not take any of the styling or color.
     * @param component the component to set as text
     */
    default void setText(@NotNull Component component) {
        setText(component, NamedTextColor.WHITE);
    }

    /**
     * This is different from {@link #setText(String)} as it accepts a {@link Component} instead of a plain string.
     * This will automatically serialize the component to plain text including resolving translations.
     *
     * @apiNote This does not take any of the styling or color.
     * @param component the component to set as text
     * @param color the color to set the text to
     */
    default void setText(@NotNull Component component, @NotNull TextColor color) {
        setText(PlainTextComponentSerializer.plainText().serialize(component), color);
    }

    default void setText(@NotNull String text) {
        setText(text, NamedTextColor.WHITE);
    }

    void setText(@NotNull String text, @NotNull TextColor color);

}
