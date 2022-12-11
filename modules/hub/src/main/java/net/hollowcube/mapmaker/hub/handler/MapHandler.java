package net.hollowcube.mapmaker.hub.handler;

import net.hollowcube.mapmaker.result.FutureResult;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface MapHandler {

    @NotNull FutureResult<Void> createMap(@NotNull Player player, @NotNull String name);

    @NotNull FutureResult<Void> editMap(@NotNull String mapId, @NotNull Player player);

    @NotNull FutureResult<Void> playMap(@NotNull String mapId, @NotNull Player player);

    @NotNull FutureResult<Void> infoMap(@NotNull String mapId, @NotNull Player player);

}
