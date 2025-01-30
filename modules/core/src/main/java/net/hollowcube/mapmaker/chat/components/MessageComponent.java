package net.hollowcube.mapmaker.chat.components;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public record MessageComponent(
        @NotNull Component text,
        boolean ping
) {

    public static MessageComponent of(@NotNull Component text) {
        return new MessageComponent(text, false);
    }

    public static MessageComponent of(@NotNull Component text, boolean ping) {
        return new MessageComponent(text, ping);
    }
}
