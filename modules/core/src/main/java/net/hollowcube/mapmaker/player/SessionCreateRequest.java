package net.hollowcube.mapmaker.player;

import org.jetbrains.annotations.NotNull;

public record SessionCreateRequest(
        @NotNull String server,
        @NotNull String username,
        @NotNull String ip
) {
}
