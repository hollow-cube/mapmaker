package net.hollowcube.mapmaker.hub.handler;

import net.hollowcube.mapmaker.map.MapHandle;
import net.hollowcube.mapmaker.map.MapManager;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.storage.MapStorage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MapHandlerImpl implements MapHandler {
    protected final MapStorage storage;
    protected final MapManager maps;

    public MapHandlerImpl(MapStorage storage, MapManager maps) {
        this.storage = storage;
        this.maps = maps;
    }

    @Override
    public @NotNull CompletableFuture<MapData> createMap(@NotNull Player player, MapData.@NotNull Type type, @NotNull String name) {
        var map = new MapData(UUID.randomUUID().toString(), type);
        map.setName(name);
        return storage.createMap(map)
                .thenApply(map1 -> {
                    player.sendMessage(
                            Component.text("Successfully created ", NamedTextColor.WHITE)
                                    .append(Component.text(map1.name(), NamedTextColor.AQUA).clickEvent(ClickEvent.copyToClipboard(map1.id()))));
                    System.out.println("Created map " + map.id());
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
}
