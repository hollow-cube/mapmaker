package net.hollowcube.chat;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.UUID;

public record ChatMessage(
        @NotNull Instant timestamp,
        @NotNull String serverId,
        @NotNull String context,
        @NotNull UUID sender,
        @NotNull String message
) {

}
