package net.hollowcube.mapmaker.player;

import org.jetbrains.annotations.NotNull;

public record SessionTransferRequest(
        @NotNull String server,

        // Presence info
        @NotNull String type,
        @NotNull String state,
        @NotNull String map
) {
}
