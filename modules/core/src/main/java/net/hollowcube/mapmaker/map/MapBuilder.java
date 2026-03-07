package net.hollowcube.mapmaker.map;

import org.jetbrains.annotations.NotNull;

public record MapBuilder(@NotNull String mapId, @NotNull String playerId, boolean pending) {
}
