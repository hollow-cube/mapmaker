package net.hollowcube.mapmaker.hub;

import io.prometheus.client.Histogram;
import net.hollowcube.mapmaker.event.MapDeletedEvent;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.model.PlayerData;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

/**
 * Handles hub actions. Particularly, it handles the actions/effects of particular actions (such as creating a map),
 * however it does <i>not</i> handle presenting the results to a player. This job is left to GUIs and/or commands/chat.
 */
public class HubHandler {
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


    private final MapService mapService;
    private final HubServer server;

    public HubHandler(@NotNull HubServer server, @NotNull MapService mapService) {
        this.server = server;
        this.mapService = mapService;
    }

    @Blocking
    public @NotNull MapData createMapForPlayer(@NotNull String owner) {
        try (var ignored = createMapTime.startTimer()) {
            return mapService.createMap(owner, owner);
        }
    }

    @Blocking
    public @NotNull MapData createMapForPlayerInSlot(@NotNull PlayerData playerData, int slot) {
        try (var ignored = createMapForPlayerInSlotTime.startTimer()) {

            // Ensure selected slot is available
            var slotState = playerData.getSlotState(slot);
            if (slotState == PlayerData.SLOT_STATE_LOCKED)
                throw new MapSlotLockedError();
            if (slotState == PlayerData.SLOT_STATE_IN_USE)
                throw new MapSlotInUseError();

            // The updating player slot and creating map actions need to happen as a saga or 2pc at minimum
            var map = createMapForPlayer(playerData.getId());
            playerData.setMapSlot(slot, map.id());
            server.playerStorage().updatePlayer(playerData);

            return map;
        }
    }

//
//    /**
//     * Deletes a map given its ID (using the given player as the one deleting it). Does the following:
//     * <ul>
//     *     <li>Fetch map</li>
//     *     <li>Check for admin permission on map</li>
//     *     <li>Fetch next short id</li>
//     *     <li>Update map data (set to published, add short id)</li>
//     *     <li>Unlink map from players</li>
//     *     <li>Add all players as viewers</li>
//     *     <li>Save map</li>
//     * </ul>
//     */
//    public @NotNull MapData publishMap(@NotNull String playerId, @NotNull String mapId) {
//        try (var ignored = publishMapTime.startTimer()) {
//            var map = server.mapStorage().getMapById(mapId);
//
//            var hasPermission = server.mapPermissions().checkPermission(map.getId(), playerId, MapData.Permission.ADMIN);
//            if (!hasPermission)
//                throw new RuntimeException("todo set a better error");
//
//            map.setPublishedId(server.mapStorage().getNextId());
//            map.setPublishedAt(Instant.now());
//
//            //todo delete is a deceptive name, it actually just marks a map as gone from slot
//            EventDispatcher.call(new MapDeletedEvent(map.getId()));
//
//            // Make the map public
//            server.mapPermissions().makeMapPublic(mapId);
//            server.mapStorage().updateMap(map);
//
//            return map;
//        }
//    }
//
//    public void playMap(@NotNull Player player, @NotNull String mapId) {
//        try (var ignored = playMapTime.startTimer()) {
//            var playerData = PlayerData.fromPlayer(player);
//            var map = server.mapStorage().getMapById(mapId);
//
//            if (!map.isPublished())
//                throw new MapNotPublishedError();
//
//            var hasPermission = server.mapPermissions().checkPermission(mapId, playerData.getId(), MapData.Permission.READ);
//            if (!hasPermission) {
//                throw new RuntimeException("blah balh blah");
//            }
//
//            server.bridge().joinMap(player, mapId, false);
//        }
//    }

    public void editMap(@NotNull Player player, @NotNull String mapId) {
        try (var ignored = editMapTime.startTimer()) {
            var playerData = PlayerData.fromPlayer(player);
            var map = server.mapService().getMap(playerData.getId(), mapId);

            if (map.isPublished())
                // todo you should perhaps just lose editing permission?
                throw new MapIsPublishedError();

            server.bridge().joinMap(player, mapId, true);
        }
    }

//    public static class MapNotPublishedError extends RuntimeException {
//    }

    public static class MapIsPublishedError extends RuntimeException {
    }

    public static class MapSlotLockedError extends RuntimeException {
    }

    public static class MapSlotInUseError extends RuntimeException {
    }

//    public static class MapMissingOwnerError extends RuntimeException {
//    }
//
//    public static class InvalidMapNameError extends RuntimeException {
//    }

}
