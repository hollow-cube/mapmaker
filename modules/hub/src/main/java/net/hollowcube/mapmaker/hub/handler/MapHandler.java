package net.hollowcube.mapmaker.hub.handler;

import net.hollowcube.mapmaker.model.MapData;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public interface MapHandler {

    @NotNull CompletableFuture<MapData> createMap(@NotNull Player player, @NotNull String name);

    @NotNull CompletableFuture<Void> editMap(@NotNull String mapId, @NotNull Player player);

    @NotNull CompletableFuture<Void> playMap(@NotNull String mapId, @NotNull Player player);

    @NotNull CompletableFuture<Void> infoMap(@NotNull String mapId, @NotNull Player player);

}
