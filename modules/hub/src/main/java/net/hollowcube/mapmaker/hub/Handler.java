package net.hollowcube.mapmaker.hub;

import io.prometheus.client.Histogram;
import net.hollowcube.common.result.Error;
import net.hollowcube.common.result.FutureResult;
import net.hollowcube.common.result.Result;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.model.PlayerData;
import net.hollowcube.mapmaker.storage.MapStorage;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

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

    public static final Error ERR_MAP_NOT_FOUND = Error.of("map not found");
    public static final Error ERR_MAP_NOT_PUBLISHED = Error.of("map not published");
    public static final Error ERR_MAP_IS_PUBLISHED = Error.of("map is published");
    public static final Error ERR_SLOT_LOCKED = Error.of("slot locked");
    public static final Error ERR_SLOT_IN_USE = Error.of("slot in use");
    public static final Error ERR_MISSING_OWNER = Error.of("missing owner"); //todo doesnt need to be defined here, not meant to be handled
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
        var timer = createMapTime.startTimer();

        //todo actually lookup the owner and make sure they exist
        if (map.getOwner() == null)
            return FutureResult.error(ERR_MISSING_OWNER);
        if (!map.getName().matches(MapData.NAME_REGEX))
            return FutureResult.error(ERR_INVALID_MAP_NAME);

        map.setId(UUID.randomUUID().toString()); // Create random ID
        map.setPublishedAt(null); // Sanity check
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
                })
                .alsoRaw(unused -> timer.observeDuration());
    }

    /**
     * Creates the given map for the given player in the given slot.
     * <p>
     * The owner of the map is always set to the given {@link PlayerData}s ID.
     * <p>
     * If the slot is in use, ERR_SLOT_IN_USE is returned.
     */
    public @NotNull FutureResult<MapData> createMapForPlayerInSlot(@NotNull PlayerData playerData, @NotNull MapData map, int slot) {
        var timer = createMapForPlayerInSlotTime.startTimer();

        // Ensure selected slot is available
        var slotState = playerData.getSlotState(slot);
        if (slotState == PlayerData.SLOT_STATE_LOCKED)
            return FutureResult.error(ERR_SLOT_LOCKED);
        if (slotState == PlayerData.SLOT_STATE_IN_USE)
            return FutureResult.error(ERR_SLOT_IN_USE);

        map.setOwner(playerData.getId());
        map.setPublishedAt(null); // Sanity check

        return createMap(map)
                // Set the map in the given player slot & save player
                .flatMap(map1 -> {
                    playerData.setMapSlot(slot, map1.getId());
                    return server.playerStorage().updatePlayer(playerData)
                            .mapErr(err -> Result.error(err.wrap("failed to update player: {}")))
                            .map(unused -> map1);
                })
                .alsoRaw(unused -> timer.observeDuration());
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
        var timer = deleteMapTime.startTimer();

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
                })
                .alsoRaw(unused -> timer.observeDuration());
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
    public @NotNull FutureResult<MapData> publishMap(@NotNull String playerId, @NotNull String mapId) {
        var timer = publishMapTime.startTimer();

        return server.mapStorage().getMapById(mapId)
                // Check admin permissions
                .flatAlso(map -> server.mapPermissions().checkPermission(map.getId(), playerId, MapData.ADMIN)
                        .wrapErr("failed to check admin permission: {}"))
                // Fetch next short id & update map
                .flatMap(map -> server.mapStorage().getNextId()
                        .map(shortId -> {
                            map.setPublishedAt(Instant.now());
                            map.setPublishedId(shortId);
                            return map;
                        }))
                // Unlink map from all players
                .flatAlso(map -> server.playerStorage().unlinkMap(mapId)
                        .wrapErr("failed to unlink map from players: {}"))
                // Add all players as viewers
                .flatAlso(map -> server.mapPermissions().makeMapPublic(mapId)
                        .wrapErr("failed to make map public: {}"))
                // Save map
                .flatAlso(map -> server.mapStorage().updateMap(map)
                        .wrapErr("failed to update map: {}"))
                .mapErr(err -> {
                    if (err.is(MapStorage.ERR_NOT_FOUND))
                        return Result.error(ERR_MAP_NOT_FOUND);
                    return Result.error(err.wrap("failed to publish map: {}"));
                })
                .alsoRaw(unused -> timer.observeDuration());
    }

    public @NotNull FutureResult<Void> playMap(@NotNull Player player, @NotNull String mapId) {
        var timer = playMapTime.startTimer();
        var playerData = PlayerData.fromPlayer(player);
        return server.mapStorage().getMapById(mapId)
                // Ensure map is published and check permission
                .flatMap(map -> {
                    if (!map.isPublished())
                        return FutureResult.error(ERR_MAP_NOT_PUBLISHED);

                    return server.mapPermissions().checkPermission(mapId, playerData.getId(), MapData.READ)
                            .wrapErr("failed to check read permission: {}")
                            .map(unused -> map);
                })
                // Send player to map instance
                .flatMap(map -> server.bridge().joinMap(player, mapId, false)
                        .wrapErr("failed to join map: {}"))
                .alsoRaw(unused -> timer.observeDuration());
    }

    public @NotNull FutureResult<Void> editMap(@NotNull Player player, @NotNull String mapId) {
        var timer = editMapTime.startTimer();
        var playerData = PlayerData.fromPlayer(player);
        return server.mapStorage().getMapById(mapId)
                // Ensure map is not published and check write permission
                .flatMap(map -> {
                    if (map.isPublished())
                        return FutureResult.error(ERR_MAP_IS_PUBLISHED);

                    return server.mapPermissions().checkPermission(mapId, playerData.getId(), MapData.WRITE)
                            .wrapErr("failed to check write permission: {}")
                            .map(unused -> map);
                })
                // Send player to map instance
                .flatMap(map -> server.bridge().joinMap(player, mapId, true)
                        .wrapErr("failed to join map: {}"))
                .alsoRaw(unused -> timer.observeDuration());
    }

}
