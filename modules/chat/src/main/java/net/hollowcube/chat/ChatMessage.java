package net.hollowcube.chat;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;

public record ChatMessage(
        @NotNull Instant timestamp,
        @NotNull String serverId,
        @NotNull String context,
        @NotNull String sender,
        @NotNull String message
) {
    public static final String DEFAULT_CONTEXT = "global";
    public static final String COMMAND_CONTEXT = "command";

}
