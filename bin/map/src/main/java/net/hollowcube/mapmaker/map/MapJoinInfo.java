package net.hollowcube.mapmaker.map;

import org.jetbrains.annotations.NotNull;

public record MapJoinInfo(
        @NotNull String playerId,
        @NotNull String mapId,
        @NotNull String state // building, etc
) {
}
