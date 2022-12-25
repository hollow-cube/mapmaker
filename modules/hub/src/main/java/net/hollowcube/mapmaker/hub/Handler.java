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
    public static final Error ERR_SLOT_NOT_IN_USE = Error.of("slot in use"); //todo unused




    public static final Error ERR_MAP_NOT_FOUND = Error.of("map not found");
    public static final Error ERR_SLOT_LOCKED = Error.of("slot locked");
    public static final Error ERR_SLOT_IN_USE = Error.of("slot in use");
    public static final Error ERR_MISSING_OWNER = Error.of("missing owner");
    public static final Error ERR_INVALID_MAP_NAME = Error.of("invalid name");

    private final HubServer server;

    public Handler(@NotNull HubServer server) {
        this.server = server;
    }

    /**
     * Creates and sets up permissions for the given map.
     *
     * @apiNote The `owner` field _must_ be set. If not, ERR_MISSING_OWNER is returned.
     * <p>
     * The map ID is overwritten with a new UUID no matter the content.
     * The return value should be used to determine the ID.
     */
    public @NotNull FutureResult<MapData> createMap(@NotNull MapData map) {
        //todo actually lookup the owner and make sure they exist
        if (map.getOwner() == null)
            return FutureResult.error(ERR_MISSING_OWNER);
        if (!map.getName().matches(MapData.NAME_REGEX))
            return FutureResult.error(ERR_INVALID_MAP_NAME);

        map.setId(UUID.randomUUID().toString()); // Create random ID
        map.setPublished(false); // Sanity check
        return server.mapStorage().createMap(map)
                // Add permissions
                .flatMap(map1 -> server.mapPermissions().addMapOwner(map1.getId(), map1.getOwner())
                        .mapErr(err -> Result.error(err.wrap("failed to add owner to map permissions: {}")))
                        .map(unused -> map1))
                // Retry on failure + wrap error
                .flatMapErr(err -> {
                    if (err.is(MapStorage.ERR_DUPLICATE_ENTRY))
                        return createMap(map); // Try again w/ new ID

                    return FutureResult.error(err.wrap("failed to create map: {}"));
                });
    }

    /**
     * Creates the given map for the given player in the given slot.
     * <p>
     * The owner of the map is always set to the given {@link PlayerData}s ID.
     * <p>
     * If the slot is in use, ERR_SLOT_IN_USE is returned.
     */
    public @NotNull FutureResult<MapData> createMapForPlayerInSlot(@NotNull PlayerData playerData, @NotNull MapData map, int slot) {
        // Ensure selected slot is available
        var slotState = playerData.getSlotState(slot);
        if (slotState == PlayerData.SLOT_STATE_LOCKED)
            return FutureResult.error(ERR_SLOT_LOCKED);
        if (slotState == PlayerData.SLOT_STATE_IN_USE)
            return FutureResult.error(ERR_SLOT_IN_USE);

        map.setOwner(playerData.getId());
        map.setPublished(false); // Sanity check

        return createMap(map)
                // Set the map in the given player slot & save player
                .flatMap(map1 -> {
                    playerData.setMapSlot(slot, map1.getId());
                    return server.playerStorage().updatePlayer(playerData)
                            .mapErr(err -> Result.error(err.wrap("failed to update player: {}")))
                            .map(unused -> map1);
                });
    }

    /**
     * Deletes a map by its ID. Does the following:
     * <ol>
     * <li>Delete the map object from Mongo</li>
     * <li>Delete all relationships referencing the map from SpiceDB</li>
     * <li>Find all players (for now just 1, but with trusted maps more) who have a record referencing that map</li>
     * <li>Update each player individually (after removing the reference to the map)</li>
     * </ol>
     */
    public @NotNull FutureResult<MapData> deleteMap(@NotNull String mapId) {
        //todo there is a missing permission check here, you could delete any map if you knew the ID
        return server.mapStorage().deleteMap(mapId)
                // Delete all associated permissions
                .flatMap(map -> server.mapPermissions().deleteMap(mapId)
                        .mapErr(err -> Result.error(err.wrap("failed to delete map permissions: {}")))
                        .map(unused -> map))
                // Delete from mongo
                .flatMap(map -> server.playerStorage().unlinkMap(mapId)
                        .mapErr(err -> Result.error(err.wrap("failed to unlink map from players: {}")))
                        .map(unused -> map))
                .mapErr(err -> {
                    if (err.is(MapStorage.ERR_NOT_FOUND))
                        return Result.error(ERR_MAP_NOT_FOUND);
                    return Result.error(err.wrap("failed to delete map: {}"));
                });
    }


    //todo should not take a slot. Just a playerid and mapid
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

    //todo delete me
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

    //todo delete me, should be replaced by a playMap(Player, mapId)
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

    //todo delete me, can just be done with a call to map storage
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
