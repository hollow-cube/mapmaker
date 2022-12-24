package net.hollowcube.mapmaker.hub;

import net.hollowcube.common.result.Error;
import net.hollowcube.common.result.FutureResult;
import net.hollowcube.common.result.Result;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.model.PlayerData;
import net.hollowcube.mapmaker.storage.MapStorage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Handles hub actions. Particularly, it handles the actions/effects of particular actions (such as creating a map),
 * however it does <i>not</i> handle presenting the results to a player. This job is left to GUIs and/or commands/chat.
 */
public class Handler {
    public static final Error ERR_SLOT_IN_USE = Error.of("slot in use");
    public static final Error ERR_SLOT_NOT_IN_USE = Error.of("slot in use");
    public static final Error ERR_DUPLICATE_NAME = Error.of("duplicate name");

    private final HubServer server;

    public Handler(@NotNull HubServer server) {
        this.server = server;
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
        map.setOwner(playerData.getId());
        map.setName(name);
        return server.mapStorage().createMap(map)
                .flatMap(map1 -> {
                    // Update playerdata (todo this should be done as a transaction)
                    playerData.setMapSlot(slot, map.getId());
                    return server.playerStorage().updatePlayer(playerData)
                            .mapErr(err -> Result.error(err.wrap("failed to update player data: {}")))
                            .map(unused -> map1);
                })
                // Set player as owner of the map
                .flatMap(map1 -> server.mapPermissions().addMapOwner(map1.getId(), playerData.getId())
                        .mapErr(err -> Result.error(err.wrap("failed to set player as owner of map: {}")))
                        .map(unused -> map1))
                .flatMapErr(err -> {
                    if (err.is(MapStorage.ERR_DUPLICATE_NAME))
                        return FutureResult.error(ERR_DUPLICATE_NAME);

                    // If the ID was in use, attempt to create it again
                    if (err.is(MapStorage.ERR_DUPLICATE_ENTRY))
                        return createMap(player, name, slot);

                    return FutureResult.error(err.wrap("failed to create map: {0}"));
                });
    }

    public @NotNull FutureResult<Void> publishMap(@NotNull Player player, int slot) {
        var playerData = PlayerData.fromPlayer(player);
        var mapId = playerData.getMapSlot(slot);
        if (mapId == null) return FutureResult.error(ERR_SLOT_NOT_IN_USE);

        return server.mapStorage().getNextId()
                .flatMap(publishedId -> server.mapStorage().getMapById(mapId)
                        .flatMap(map -> {
                            map.setPublished(true);
                            map.setPublishedId(publishedId);
                            playerData.setMapSlot(slot, null);
                            return server.mapStorage().updateMap(map)
                                    .flatMap(unused -> server.playerStorage().updatePlayer(playerData));
                        })
                        .mapErr(err -> Result.error(err.wrap("failed to publish map: {0}"))));
    }

    public @NotNull FutureResult<Void> editMap(@NotNull String nameOrId, @NotNull Player player) {
        var playerData = PlayerData.fromPlayer(player);
        return server.mapStorage().getPlayerMap(playerData.getId(), nameOrId)
                .map(map -> {
                    player.sendMessage("Editing map " + map.getName());
                    return map;
                })
                .flatMap(map -> server.bridge().joinMap(player, map.getId(), true))
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

    public @NotNull FutureResult<Void> editMap2(@NotNull Player player, @NotNull String mapId) {
        var playerData = PlayerData.fromPlayer(player);
        return server.mapPermissions().checkPermission(mapId, playerData.getId(), MapData.WRITE)
                .flatMap(unused -> {
                    // Player has permission, send them to the map
                    return server.bridge().joinMap(player, mapId, true);
                });
    }

    public @NotNull FutureResult<Void> playMap(@NotNull String nameOrId, @NotNull Player player) {
        var playerData = PlayerData.fromPlayer(player);
        return server.mapStorage().getPlayerMap(playerData.getId(), nameOrId)
                .map(map -> {
                    player.sendMessage("Playing map " + map.getName());
                    return map;
                })
                .flatMap(map -> server.bridge().joinMap(player, map.getId(), false))
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
        var playerData = PlayerData.fromPlayer(player);
        return server.mapStorage().getPlayerMap(playerData.getId(), nameOrId)
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
