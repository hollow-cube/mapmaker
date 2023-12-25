package net.hollowcube.mapmaker.player;

import org.jetbrains.annotations.NotNull;

public record JoinHubRequest(
        @NotNull String player
) {
}
