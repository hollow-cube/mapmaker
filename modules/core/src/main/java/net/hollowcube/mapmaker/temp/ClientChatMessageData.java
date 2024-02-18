package net.hollowcube.mapmaker.temp;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record ClientChatMessageData(
        @NotNull Type type,

        // CHAT_UNSIGNED
        @NotNull String sender,
        @NotNull String message,
        @NotNull String channel,
        @Nullable String currentMap
) {

    public enum Type {
        CHAT_UNSIGNED,
        CHAT_SYSTEM
    }
}
