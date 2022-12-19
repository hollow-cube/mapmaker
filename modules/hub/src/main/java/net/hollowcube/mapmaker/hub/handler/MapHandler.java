package net.hollowcube.mapmaker.hub.handler;

import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.model.PlayerData;
import net.hollowcube.mapmaker.oldtoremove.MapHandle;
import net.hollowcube.mapmaker.oldtoremove.MapManager;
import net.hollowcube.mapmaker.player.PlayerHooks;
import net.hollowcube.mapmaker.result.Error;
import net.hollowcube.mapmaker.result.FutureResult;
import net.hollowcube.mapmaker.result.Result;
import net.hollowcube.mapmaker.storage.MapStorage;
import net.hollowcube.mapmaker.storage.PlayerStorage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class MapHandler {
    public static final Logger LOGGER = LoggerFactory.getLogger(MapHandler.class);

    public static final Error ERR_SLOT_IN_USE = Error.of("slot in use");
    public static final Error ERR_DUPLICATE_NAME = Error.of("duplicate name");

    protected final MapStorage storage;
    protected final PlayerStorage playerStorage;
    protected final MapManager maps;

    public MapHandler(MapStorage storage, MapManager maps, PlayerStorage playerStorage) {
        this.storage = storage;
        this.maps = maps;
        this.playerStorage = playerStorage;
    }

    public @NotNull MapStorage storage() {
        return storage;
    }

    public @NotNull FutureResult<MapData> createMap(@NotNull Player player, @NotNull String name, int slot) {
        var playerData = PlayerData.fromPlayer(player);

        // Ensure selected slot is available
        var slotMap = playerData.getMapSlot(slot);
        if (slotMap != null) {
            return FutureResult.error(ERR_SLOT_IN_USE);
        }

        // Create map
        var map = new MapData();
        map.setId(UUID.randomUUID().toString());
        map.setOwner(PlayerHooks.getId(player));
        map.setName(name);
        return storage.createMap(map)
                .flatMap(map1 -> {
                    // Update playerdata (todo this should be done as a transaction)
                    playerData.setMapSlot(slot, map.getId());
                    return playerStorage.updatePlayer(playerData)
                            .mapErr(err -> Result.error(err.wrap("failed to update player data: {}")))
                            .map(unused -> map1);
                })
                .flatMapErr(err -> {
                    if (err.is(MapStorage.ERR_DUPLICATE_NAME))
                        return FutureResult.error(ERR_DUPLICATE_NAME);

                    // If the ID was in use, attempt to create it again
                    if (err.is(MapStorage.ERR_DUPLICATE_ENTRY))
                        return createMap(player, name, slot);

                    return FutureResult.error(err.wrap("failed to create map: {0}"));
                });
    }

    public @NotNull FutureResult<Void> editMap(@NotNull String nameOrId, @NotNull Player player) {
        return storage.getPlayerMap(PlayerHooks.getId(player), nameOrId)
                .map(map -> {
                    player.sendMessage("Editing map " + map.getName());
                    return map;
                })
                .flatMap(map -> maps.joinMap(map, MapHandle.FLAG_EDIT, player))
                .mapErr(err -> {
                    // Specific error for map not found
                    if (err.is(MapStorage.ERR_NOT_FOUND)) {
                        player.sendMessage("Map not found: " + nameOrId);
                        return Result.ofNull();
                    }

                    // Some other error
                    player.sendMessage("Failed to join map: " + err);
                    return Result.error(err.wrap("failed to edit map: {0}"));
                });
    }

    public @NotNull FutureResult<Void> playMap(@NotNull String nameOrId, @NotNull Player player) {
        return storage.getPlayerMap(PlayerHooks.getId(player), nameOrId)
                .map(map -> {
                    player.sendMessage("Playing map " + map.getName());
                    return map;
                })
                .flatMap(map -> maps.joinMap(map, MapHandle.FLAG_NONE, player))
                .mapErr(err -> {
                    // Specific error for map not found
                    if (err.is(MapStorage.ERR_NOT_FOUND)) {
                        player.sendMessage("Map not found: " + nameOrId);
                        return Result.ofNull();
                    }

                    // Some other error
                    player.sendMessage("Failed to join map: " + err.message());
                    return Result.error(err.wrap("failed to play map: {0}"));
                });
    }

    public @NotNull FutureResult<Void> infoMap(@NotNull String nameOrId, @NotNull Player player) {
        return storage.getPlayerMap(PlayerHooks.getId(player), nameOrId)
                .then(map -> {
                    player.sendMessage(Component.text("Map info for ", NamedTextColor.WHITE)
                            .append(Component.text(map.getName(), NamedTextColor.AQUA).clickEvent(ClickEvent.copyToClipboard(map.getId()))));
                    player.sendMessage(Component.text("ID: ", NamedTextColor.WHITE)
                            .append(Component.text(map.getId(), NamedTextColor.AQUA).clickEvent(ClickEvent.copyToClipboard(map.getId()))));
                })
                .mapErr(err -> {
                    // Specific error for map not found
                    if (err.is(MapStorage.ERR_NOT_FOUND)) {
                        player.sendMessage("Map not found: " + nameOrId);
                        return Result.ofNull();
                    }

                    // Some other error
                    player.sendMessage("Failed to get map info: " + err.message());
                    return Result.error(err.wrap("failed to get map info: {0}"));
                });
    }
}
