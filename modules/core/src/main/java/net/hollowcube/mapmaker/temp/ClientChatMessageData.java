package net.hollowcube.mapmaker.temp;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RuntimeGson
public record ClientChatMessageData(
        @NotNull Type type,

        // CHAT_UNSIGNED
        @NotNull String sender,
        @NotNull String message,
        @NotNull String channel,
        @Nullable String currentMap,
        long seed
) {

    public static final String CHANNEL_GLOBAL = "global";
    public static final String CHANNEL_LOCAL = "local";
    public static final String CHANNEL_REPLY = "reply";

    public enum Type {
        CHAT_UNSIGNED,
        CHAT_SYSTEM
    }
}
