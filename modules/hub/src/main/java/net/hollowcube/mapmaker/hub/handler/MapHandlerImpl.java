package net.hollowcube.mapmaker.hub.handler;

import net.hollowcube.mapmaker.map.MapHandle;
import net.hollowcube.mapmaker.map.MapManager;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.player.PlayerHooks;
import net.hollowcube.mapmaker.result.FutureResult;
import net.hollowcube.mapmaker.result.Result;
import net.hollowcube.mapmaker.storage.MapStorage;
import net.hollowcube.mapmaker.storage.Storage;
import net.hollowcube.util.FutureUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MapHandlerImpl implements MapHandler {
    public static final Logger LOGGER = LoggerFactory.getLogger(MapHandlerImpl.class);

    protected final MapStorage storage;
    protected final MapManager maps;

    public MapHandlerImpl(MapStorage storage, MapManager maps) {
        this.storage = storage;
        this.maps = maps;
    }

    @Override
    public @NotNull FutureResult<Void> createMap(@NotNull Player player, @NotNull String name) {
        var map = new MapData();
        map.setId(UUID.randomUUID().toString());
        map.setOwner(PlayerHooks.getId(player));
        map.setName(name);
        return storage.createMap(map)
                .then(map1 -> {
                    player.sendMessage(
                            Component.text("Successfully created ", NamedTextColor.WHITE)
                                    .append(Component.text(map1.getName(), NamedTextColor.AQUA).clickEvent(ClickEvent.copyToClipboard(map1.getId()))));
                    System.out.println("Created map " + map.getId());
                })
                .flatMapErr(err -> {
                    if (err.is(MapStorage.ERR_DUPLICATE_NAME)) {
                        player.sendMessage("Map named " + name + " already exists.");
                        return FutureResult.ofNull();
                    }

                    // If the ID was in use, attempt to create it again
                    if (err.is(Storage.ERR_DUPLICATE_ENTRY)) {
                        return createMap(player, name);
                    }

                    player.sendMessage("Failed to create map: " + err.message());
                    return FutureResult.error(err.wrap("failed to create map: {0}"));
                });
    }

    @Override
    public @NotNull FutureResult<Void> editMap(@NotNull String mapId, @NotNull Player player) {
        player.sendMessage("Editing map " + mapId);
        return storage.getMapById(mapId)
                .flatMap(map -> maps.joinMap(map, MapHandle.FLAG_EDIT, player))
                .mapErr(err -> {
                    // Specific error for map not found
                    if (err.is(Storage.ERR_NOT_FOUND)) {
                        player.sendMessage("Map not found: " + mapId);
                        return Result.ofNull();
                    }

                    // Some other error
                    player.sendMessage("Failed to join map: " + err);
                    return Result.error(err.wrap("failed to edit map: {0}"));
                });
    }

    @Override
    public @NotNull FutureResult<Void> playMap(@NotNull String mapId, @NotNull Player player) {
        player.sendMessage("Playing map " + mapId);
        return storage.getMapById(mapId)
                .flatMap(map -> maps.joinMap(map, MapHandle.FLAG_NONE, player))
                .mapErr(err -> {
                    // Specific error for map not found
                    if (err.is(Storage.ERR_NOT_FOUND)) {
                        player.sendMessage("Map not found: " + mapId);
                        return Result.ofNull();
                    }

                    // Some other error
                    player.sendMessage("Failed to join map: " + err.message());
                    return Result.error(err.wrap("failed to play map: {0}"));
                });
    }

    @Override
    public @NotNull FutureResult<Void> infoMap(@NotNull String mapId, @NotNull Player player) {
        return storage.getMapById(mapId)
                .then(map -> {
                    player.sendMessage(Component.text("Map info for ", NamedTextColor.WHITE)
                            .append(Component.text(map.getName(), NamedTextColor.AQUA).clickEvent(ClickEvent.copyToClipboard(map.getId()))));
                    player.sendMessage(Component.text("ID: ", NamedTextColor.WHITE)
                            .append(Component.text(map.getId(), NamedTextColor.AQUA).clickEvent(ClickEvent.copyToClipboard(map.getId()))));
                })
                .mapErr(err -> {
                    // Specific error for map not found
                    if (err.is(Storage.ERR_NOT_FOUND)) {
                        player.sendMessage("Map not found: " + mapId);
                        return Result.ofNull();
                    }

                    // Some other error
                    player.sendMessage("Failed to get map info: " + err.message());
                    return Result.error(err.wrap("failed to get map info: {0}"));
                });
    }
}
