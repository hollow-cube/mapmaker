package net.hollowcube.map.world;

import net.hollowcube.map.MapHooks;
import net.hollowcube.map.MapServer;
import net.hollowcube.map.event.MapWorldRegisterEvent;
import net.hollowcube.map.event.MapWorldUnregisterEvent;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.model.SaveState;
import net.hollowcube.mapmaker.player.PlayerHooks;
import net.hollowcube.mapmaker.result.FutureResult;
import net.hollowcube.mapmaker.storage.SaveStateStorage;
import net.hollowcube.mapmaker.util.ExtraTags;
import net.hollowcube.mapmaker.util.StaticAbuse;
import net.hollowcube.util.FutureUtil;
import net.hollowcube.world.BaseWorld;
import net.hollowcube.world.event.PlayerInstanceLeaveEvent;
import net.hollowcube.world.event.PlayerSpawnInInstanceEvent;
import net.hollowcube.world.generation.MapGenerators;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MapWorld extends BaseWorld {
    private static final Logger logger = LoggerFactory.getLogger(MapWorld.class);

    public static final Tag<String> MAP_ID = Tag.String("mapmaker:map/id");
    public static final Tag<MapData> MAP_DATA = ExtraTags.Transient("mapmaker:map/data");

    private static final Tag<MapWorld> THIS_TAG = ExtraTags.Transient("mapmaker:map/world");

    public static @NotNull MapWorld fromInstance(@NotNull Instance instance) {
        return Objects.requireNonNull(instance.getTag(THIS_TAG));
    }

    public static final int FLAG_NONE = 0;
    public static final int FLAG_EDIT = 1;

    private MapServer mapServer;
    private final MapData map;
    private final int flags;

    public MapWorld(@NotNull MapServer mapServer, @NotNull MapData map, int flags) {
        super(mapServer.worldManager(), map.getId());
        this.mapServer = mapServer;
        this.map = map;
        this.flags = flags;

        instance().getWorldBorder().setDiameter(100); //todo
        instance().setGenerator(MapGenerators.flatWorld());

        instance().setTag(THIS_TAG, this);
        instance().setTag(MAP_ID, map.getId());
        instance().setTag(MAP_DATA, map);

        var eventNode = instance().eventNode();
        if ((flags & FLAG_EDIT) != 0) {
            // Editing
            eventNode.addListener(PlayerSpawnInInstanceEvent.class, this::initPlayerForEditing);
        } else {
            // Playing
            eventNode.addListener(PlayerBlockBreakEvent.class, this::preventBlockBreak);
            eventNode.addListener(PlayerBlockPlaceEvent.class, this::preventBlockPlace);

            eventNode.addListener(PlayerSpawnInInstanceEvent.class, this::initPlayerForPlaying);
        }

        // Handle the last person leaving
        eventNode.addListener(PlayerInstanceLeaveEvent.class, this::handlePlayerLeave);

        // Mark the map as registered
        EventDispatcher.call(new MapWorldRegisterEvent(this));
    }

    public @NotNull MapData map() {
        return map;
    }

    public int flags() {
        return flags;
    }

    @Override
    public @NotNull CompletableFuture<Void> loadWorld() {
        return super.loadWorld();
    }

    @Override
    public @NotNull CompletableFuture<@NotNull String> saveWorld() {
        //todo handle failure here (probably write to disk and keep trying to save or write somewhere else or something)
        return super.saveWorld()
                .thenCompose(fileId -> {
                    map.setMapFileId(fileId);
                    return StaticAbuse.mapStorage.updateMap(map)
                            .toCompletableFuture()
                            // Still need to return the file id.
                            .thenApply(unused -> fileId);
                });
    }

    @Override
    public @NotNull CompletableFuture<Void> unloadWorld() {
        return super.unloadWorld()
                .thenRun(() -> EventDispatcher.call(new MapWorldUnregisterEvent(this)));
    }

    private void initPlayerForEditing(@NotNull PlayerSpawnInInstanceEvent event) {
        var player = event.getPlayer();
        player.teleport(map.getSpawnPoint());
        player.setGameMode(GameMode.CREATIVE);

        player.sendMessage("Now editing " + map.getName());
    }

    private void initPlayerForPlaying(@NotNull PlayerSpawnInInstanceEvent event) {
        var player = event.getPlayer();
        var playerId = PlayerHooks.getId(player);

        //todo this wont work really. Need to hold the player somewhere while we load their savestate
        var saveStates = mapServer.saveStateStorage();
        saveStates.getLatestSaveState(playerId, map.getId())
                .flatMapErr(err -> {
                    if (err.is(SaveStateStorage.ERR_NOT_FOUND)) {
                        // Create savestate if not exists
                        var saveState = new SaveState();
                        saveState.setId(UUID.randomUUID().toString());
                        saveState.setPlayerId(playerId);
                        saveState.setMapId(map.getId());
                        saveState.setStartTime(Instant.now());
                        saveState.setPos(map.getSpawnPoint());
                        return saveStates.createSaveState(saveState);
                    }

                    // Any other error
                    return FutureResult.error(err);
                })
                .then(saveState -> {
                    player.setTag(MapHooks.PLAYING, true);
                    player.setTag(SaveState.TAG, saveState);

                    player.teleport(saveState.getPos());
                    player.setGameMode(GameMode.ADVENTURE);
                    player.setAllowFlying(true);

                    player.sendMessage("Now playing " + map.getName());
                })
                .thenErr(err -> {
                    logger.error("Failed to load save state for player {} in map {}: {}", playerId, map.getId(), err);
                    player.kick("failed to load save state: " + err.message());
                });
    }

    private void preventBlockBreak(PlayerBlockBreakEvent event) {
        event.setCancelled(true);
    }

    private void preventBlockPlace(PlayerBlockPlaceEvent event) {
        event.setCancelled(true);
    }

    private void handlePlayerLeave(@NotNull PlayerInstanceLeaveEvent event) {
        // Always remove playing tag if present
        var player = event.getPlayer();
        player.removeTag(MapHooks.PLAYING);

        // Save savestate
        var saveState = player.getTag(SaveState.TAG);
        player.removeTag(SaveState.TAG);
        saveState.setPos(player.getPosition());
        mapServer.saveStateStorage().updateSaveState(saveState)
                .thenErr(err -> {
                    logger.error("Failed to save save state for player {} in map {}: {}",
                            PlayerHooks.getId(player), map.getId(), err);
                });

        // Handle unloading the world when the last player leaves
        //todo need to immediately unregister the world from the world manager so that no players are added.
        //todo what happens if a bid is sent and then the last player leaves? do we track that a bid is out?
        // or just tell the player an error occurred and send them back
        // 1) Require the hub to send a message indicating which bid was selected, hold the server until that message
        //    is received. During this time the map is marked unready and will not be submitted for more bids until
        //    that player joins, at which point it becomes active again.

        // During event, the player is still in the instance, so we check for 1 remaining player.
        if (instance().getPlayers().size() > 1) return;

        // Must do this next tick because the player is still in the instance at call time.
        instance().scheduleNextTick(unused -> {
            // No more players, save/unload the world
            if ((flags & FLAG_EDIT) != 0) {
                // Only save
                saveAndUnloadWorld()
                        .thenRun(() -> System.out.println("Saved and unloaded world " + map.getId()))
                        .exceptionally(FutureUtil::handleException);
            } else {
                // Unload without save
                unloadWorld()
                        .thenRun(() -> System.out.println("Unloaded world " + map.getId()))
                        .exceptionally(FutureUtil::handleException);
            }
        });
    }
}
