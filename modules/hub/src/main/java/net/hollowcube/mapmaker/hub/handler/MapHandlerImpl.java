package net.hollowcube.mapmaker.hub.handler;

import net.hollowcube.mapmaker.map.MapHandle;
import net.hollowcube.mapmaker.map.MapManager;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.storage.MapStorage;
import net.hollowcube.mapmaker.storage.Storage;
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
    public @NotNull CompletableFuture<MapData> createMap(@NotNull Player player, MapData.@NotNull Type type, @NotNull String name) {
        var map = new MapData();
        map.setId(UUID.randomUUID().toString());
        map.setType(type);
        map.setName(name);
        return storage.createMap(map)
                .thenApply(map1 -> {
                    player.sendMessage(
                            Component.text("Successfully created ", NamedTextColor.WHITE)
                                    .append(Component.text(map1.getName(), NamedTextColor.AQUA).clickEvent(ClickEvent.copyToClipboard(map1.getId()))));
                    System.out.println("Created map " + map.getId());
                    return map1;
                })
                .exceptionallyCompose(e -> {
                    // If the ID was in use, attempt to create it again
                    if (e == MapStorage.DUPLICATE_ENTRY) {
                        return createMap(player, type, name);
                    }

                    player.sendMessage("Failed to create map: " + e.getMessage());
                    return CompletableFuture.failedFuture(e);
                });
    }

    @Override
    public @NotNull CompletableFuture<Void> editMap(@NotNull String mapId, @NotNull Player player) {
        player.sendMessage("Editing map " + mapId);
        return storage.getMapById(mapId)
                .thenCompose(map -> maps.joinMap(map, MapHandle.FLAG_EDIT, player))
                .exceptionally(e -> {
                    // Specific error for map not found
                    if (e == MapStorage.NOT_FOUND) {
                        player.sendMessage("Map not found: " + mapId);
                        return null;
                    }

                    // Some other error
                    player.sendMessage("Failed to join map: " + e.getMessage());
                    return null;
                });
    }

    @Override
    public @NotNull CompletableFuture<Void> infoMap(@NotNull String mapId, @NotNull Player player) {
        return storage.getMapById(mapId)
                .thenAccept(map -> {
                    //todo copilot generated this message, should refactor it
                    player.sendMessage(Component.text("Map info for ", NamedTextColor.WHITE)
                            .append(Component.text(map.getName(), NamedTextColor.AQUA).clickEvent(ClickEvent.copyToClipboard(map.getId()))));
                    player.sendMessage(Component.text("ID: ", NamedTextColor.WHITE)
                            .append(Component.text(map.getId(), NamedTextColor.AQUA).clickEvent(ClickEvent.copyToClipboard(map.getId()))));
                })
                .exceptionally(e -> {
                    // Specific error for map not found
                    if (e == Storage.NOT_FOUND) {
                        player.sendMessage("Map not found: " + mapId);
                        return null;
                    }

                    // Some other error
                    player.sendMessage("Failed to get map info: " + e.getMessage());
                    LOGGER.error("Failed to get map info", e);
                    return null;
                });
    }
}
