package net.hollowcube.mapmaker.player;

import org.jetbrains.annotations.NotNull;

public record SessionCreateRequestV2(
        @NotNull String proxy,
        @NotNull String username,
        @NotNull String ip
) {
}
