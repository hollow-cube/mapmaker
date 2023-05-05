package net.hollowcube.mapmaker.hub;

import io.prometheus.client.Histogram;
import net.hollowcube.mapmaker.event.MapDeletedEvent;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.model.PlayerData;
import net.hollowcube.mapmaker.storage.MapStorage;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import javax.management.RuntimeErrorException;
import java.time.Instant;
import java.util.UUID;

/**
 * Handles hub actions. Particularly, it handles the actions/effects of particular actions (such as creating a map),
 * however it does <i>not</i> handle presenting the results to a player. This job is left to GUIs and/or commands/chat.
 */
public class Handler {
    private static final Histogram createMapTime = Histogram.build()
            .namespace("mapmaker").name("create_map_time_seconds")
            .help("Histogram for the time it takes to create a map")
            .register();
    private static final Histogram createMapForPlayerInSlotTime = Histogram.build()
            .namespace("mapmaker").name("create_map_for_player_in_slot_time_seconds")
            .help("Histogram for the time it takes to create a map and assign it to a player in a slot")
            .register();
    private static final Histogram deleteMapTime = Histogram.build()
            .namespace("mapmaker").name("delete_map_time_seconds")
            .help("Histogram for the time it takes to delete a map")
            .register();
    private static final Histogram publishMapTime = Histogram.build()
            .namespace("mapmaker").name("publish_map_time_seconds")
            .help("Histogram for the time it takes to publish a map")
            .register();
    private static final Histogram playMapTime = Histogram.build()
            .namespace("mapmaker").name("play_map_time_seconds")
            .help("Histogram for the time it takes to join a map for playing")
            //todo should add labels for whether it found an existing map vs had to start one
            .register();
    private static final Histogram editMapTime = Histogram.build()
            .namespace("mapmaker").name("edit_map_time_seconds")
            .help("Histogram for the time it takes to join a map for editing")
            .register();

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
    public @Blocking @NotNull MapData createMap(@NotNull MapData map) {
        try (var ignored = createMapTime.startTimer()) {
            //todo actually lookup the owner and make sure they exist
            if (map.getOwner() == null)
                throw new MapMissingOwnerError();
            if (!map.getName().matches(MapData.NAME_REGEX))
                throw new InvalidMapNameError();

            map.setId(UUID.randomUUID().toString()); // Create random ID
            map.setPublishedAt(null); // Sanity check

            // All of below has bad failure cases :(
            map = server.mapStorage().createMap(map);
            server.mapPermissions().addMapOwner(map.getId(), map.getOwner());
            return map;
        }
    }

    /**
     * Creates the given map for the given player in the given slot.
     * <p>
     * The owner of the map is always set to the given {@link PlayerData}s ID.
     * <p>
     * If the slot is in use, ERR_SLOT_IN_USE is returned.
     */
    public @Blocking @NotNull MapData createMapForPlayerInSlot(@NotNull PlayerData playerData, @NotNull MapData map, int slot) {
        try (var ignored = createMapForPlayerInSlotTime.startTimer()) {

            // Ensure selected slot is available
            var slotState = playerData.getSlotState(slot);
            if (slotState == PlayerData.SLOT_STATE_LOCKED)
                throw new MapSlotLockedError();
            if (slotState == PlayerData.SLOT_STATE_IN_USE)
                throw new MapSlotInUseError();

            map.setOwner(playerData.getId());
            map.setPublishedAt(null); // Sanity check

            // More bad failure cases :(
            map = createMap(map);

            playerData.setMapSlot(slot, map.getId());
            server.playerStorage().updatePlayer(playerData);

            return map;
        }
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
    public @NotNull MapData deleteMap(@NotNull String mapId) {
        //todo there is a missing permission check here, you could delete any map if you knew the ID
        try (var ignored = deleteMapTime.startTimer()) {
            var map = server.mapStorage().deleteMap(mapId);
            server.mapPermissions().deleteMap(mapId);

            EventDispatcher.call(new MapDeletedEvent(map.getId()));

            return map;
        }
    }

    /**
     * Deletes a map given its ID (using the given player as the one deleting it). Does the following:
     * <ul>
     *     <li>Fetch map</li>
     *     <li>Check for admin permission on map</li>
     *     <li>Fetch next short id</li>
     *     <li>Update map data (set to published, add short id)</li>
     *     <li>Unlink map from players</li>
     *     <li>Add all players as viewers</li>
     *     <li>Save map</li>
     * </ul>
     */
    public @NotNull MapData publishMap(@NotNull String playerId, @NotNull String mapId) {
        try (var ignored = publishMapTime.startTimer()) {
            var map = server.mapStorage().getMapById(mapId);

            var hasPermission = server.mapPermissions().checkPermission(map.getId(), playerId, MapData.Permission.ADMIN);
            if (!hasPermission)
                throw new RuntimeException("todo set a better error");

            map.setPublishedId(server.mapStorage().getNextId());
            map.setPublishedAt(Instant.now());

            //todo delete is a deceptive name, it actually just marks a map as gone from slot
            EventDispatcher.call(new MapDeletedEvent(map.getId()));

            // Make the map public
            server.mapPermissions().makeMapPublic(mapId);
            server.mapStorage().updateMap(map);

            return map;
        }
    }

    public void playMap(@NotNull Player player, @NotNull String mapId) {
        try (var ignored = playMapTime.startTimer()) {
            var playerData = PlayerData.fromPlayer(player);
            var map = server.mapStorage().getMapById(mapId);

            if (!map.isPublished())
                throw new MapNotPublishedError();

            var hasPermission = server.mapPermissions().checkPermission(mapId, playerData.getId(), MapData.Permission.READ);
            if (!hasPermission) {
                throw new RuntimeException("blah balh blah");
            }

            server.bridge().joinMap(player, mapId, false);
        }
    }

    public void editMap(@NotNull Player player, @NotNull String mapId) {
        try (var ignored = editMapTime.startTimer();) {
            var playerData = PlayerData.fromPlayer(player);
            var map = server.mapStorage().getMapById(mapId);

            if (map.isPublished())
                //todo you should perhaps just lose editing permission?
                throw new MapIsPublishedError();

            var hasPermission = server.mapPermissions().checkPermission(mapId, playerData.getId(), MapData.Permission.WRITE);
            if (!hasPermission)
                throw new RuntimeException("blah balh blah");

            server.bridge().joinMap(player, mapId, true);
        }
    }

    public static class MapNotPublishedError extends RuntimeException { }
    public static class MapIsPublishedError extends RuntimeException { }
    public static class MapSlotLockedError extends RuntimeException { }
    public static class MapSlotInUseError extends RuntimeException { }
    public static class MapMissingOwnerError extends RuntimeException { }
    public static class InvalidMapNameError extends RuntimeException { }

}
