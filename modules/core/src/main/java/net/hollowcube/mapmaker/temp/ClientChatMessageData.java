package net.hollowcube.mapmaker.temp;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.Nullable;

@RuntimeGson
public record ClientChatMessageData(
    Type type,

    // CHAT_UNSIGNED
    String sender,
    String message,
    String channel,
    @Nullable String currentMap,
    long seed
) {

    public static final String CHANNEL_GLOBAL = "global";
    public static final String CHANNEL_LOCAL = "local";
    public static final String CHANNEL_REPLY = "reply";
    public static final String CHANNEL_STAFF = "staff";

    public enum Type {
        CHAT_UNSIGNED,
        CHAT_SYSTEM
    }
}
