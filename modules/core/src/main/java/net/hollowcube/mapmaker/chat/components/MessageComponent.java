package net.hollowcube.mapmaker.chat.components;

import net.hollowcube.common.util.RuntimeGson;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

@RuntimeGson
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
