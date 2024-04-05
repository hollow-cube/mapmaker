package net.hollowcube.mapmaker.player;

import org.jetbrains.annotations.NotNull;

public record JoinMapRequest(
        @NotNull String player,
        @NotNull String map,
        @NotNull String state,
        @NotNull String source
) {
}
