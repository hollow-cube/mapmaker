package net.hollowcube.map.world;

import net.hollowcube.canvas.RouterSection;
import net.hollowcube.common.result.FutureResult;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.map.MapHooks;
import net.hollowcube.map.MapServer;
import net.hollowcube.map.event.MapWorldCompleteEvent;
import net.hollowcube.map.event.MapWorldPlayerStartPlayingEvent;
import net.hollowcube.map.event.MapWorldPlayerStopPlayingEvent;
import net.hollowcube.map.feature.CheckpointFeature;
import net.hollowcube.map.gui.CompletedMapView;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.model.PlayerData;
import net.hollowcube.mapmaker.model.SaveState;
import net.hollowcube.mapmaker.storage.SaveStateStorage;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;

public class PlayingMapWorld extends MapWorld {
    private static final Logger logger = LoggerFactory.getLogger(PlayingMapWorld.class);

    public PlayingMapWorld(@NotNull MapServer mapServer, @NotNull MapData map) {
        super(mapServer, map);

        var eventNode = instance().eventNode();
        eventNode.addListener(PlayerBlockBreakEvent.class, this::preventBlockBreak); //todo again, BaseWorld settings
        eventNode.addListener(PlayerBlockPlaceEvent.class, this::preventBlockPlace); //todo again, BaseWorld settings
        eventNode.addListener(MapWorldCompleteEvent.class, this::handleMapCompletion);

        eventNode.addChild(new CheckpointFeature().eventNode()); //todo auto registration with selector

        if (MapHooks.isCompletable(map)) {
            eventNode.addListener(InstanceTickEvent.class, this::tickPlayers);
        }
    }

    @Override
    protected @NotNull FutureResult<Void> initPlayer(@NotNull Player player) {
        var playerId = PlayerData.fromPlayer(player).getId();

        //todo this wont work really. Need to hold the player somewhere while we load their savestate
        var saveStates = mapServer.saveStateStorage();
        return saveStates.getLatestSaveState(playerId, map.getId())
                .flatMapErr(err -> {
                    if (err.is(SaveStateStorage.ERR_NOT_FOUND)) {
                        // Create savestate if not exists
                        var saveState = new SaveState();
                        saveState.setId(UUID.randomUUID().toString());
                        saveState.setPlayerId(playerId);
                        saveState.setMapId(map.getId());
                        saveState.setStartTime(Instant.now());
                        return saveStates.createSaveState(saveState);
                    }

                    // Any other error
                    return FutureResult.error(err);
                })
                .then(saveState -> {
                    player.setTag(MapHooks.PLAYING, true);
                    player.setTag(SaveState.TAG, saveState);

                    // If exact pos is saved, teleport there. Otherwise teleport to latest checkpoint or map spawn point
                    if (saveState.getPos() != null) {
                        player.teleport(saveState.getPos()).exceptionally(FutureUtil::handleException);
                    } else if (saveState.getCheckpoint() != null) {
                        var checkpoint = map.getPoi(saveState.getCheckpoint()); //todo handle null case
                        player.teleport(new Pos(checkpoint.getPos())).exceptionally(FutureUtil::handleException);
                    } else {
                        player.teleport(map.getSpawnPoint()).exceptionally(FutureUtil::handleException);
                    }
                    player.setGameMode(GameMode.ADVENTURE);
                    player.setAllowFlying(true);

                    player.sendMessage("Now playing " + map.getName());

                    saveState.setPlaytimeUpdate(System.currentTimeMillis()); // Start timer now

                    EventDispatcher.call(new MapWorldPlayerStartPlayingEvent(this, player));
                })
                .thenErr(err -> {
                    logger.error("Failed to load save state for player {} in map {}: {}", playerId, map.getId(), err);
                    player.kick("failed to load save state: " + err.message());
                });
    }

    @Override
    protected @NotNull FutureResult<Void> savePlayer(@NotNull Player player, boolean remove) {
        var saveState = player.getTag(SaveState.TAG);
        if (remove) {
            EventDispatcher.call(new MapWorldPlayerStopPlayingEvent(this, player));
            player.removeTag(SaveState.TAG);
        }

        // Update relevant fields
        //todo should be based on a map setting
//        saveState.setPos(player.getPosition());

        // Save
        return mapServer.saveStateStorage().updateSaveState(saveState)
                .thenErr(err -> {
                    logger.error("Failed to save save state for player {} in map {}: {}",
                            PlayerData.fromPlayer(player).getId(), map.getId(), err);
                });
    }

    @Override
    protected @NotNull FutureResult<Void> closeWorld() {
        return FutureResult.wrap(unloadWorld())
                //todo handle error better.
                .thenErr(err -> logger.error("Failed to unload world: {}", err));
    }

    /** Ticks each player, updating their playtime action bar */
    private void tickPlayers(@NotNull InstanceTickEvent event) {
        var now = System.currentTimeMillis();
        instance().getPlayers().forEach(player -> {
            if (!MapHooks.isPlayerPlaying(player)) return;
            updatePlayer(player, now);
        });
    }

    private void preventBlockBreak(PlayerBlockBreakEvent event) {
        event.setCancelled(true);
    }

    private void preventBlockPlace(PlayerBlockPlaceEvent event) {
        event.setCancelled(true);
    }

    private void handleMapCompletion(@NotNull MapWorldCompleteEvent event) {
        var player = event.getPlayer();
        player.setTag(MapHooks.PLAYING, false);

        var playerData = PlayerData.fromPlayer(player);

        // Update, save, and remove savestate
        var saveState = player.getTag(SaveState.TAG);
        saveState.setCompleted(true);
        player.removeTag(SaveState.TAG);
        mapServer.saveStateStorage().updateSaveState(saveState)
                .thenErr(err -> logger.error("failed to save save state for player {}: {}", playerData.getId(), err));

        player.openInventory(new RouterSection(new CompletedMapView()).getInventory()); //todo method in MapServer to open gui with appropriate context
    }

    private void updatePlayer(@NotNull Player player, long time) {
        var saveState = SaveState.fromPlayer(player);

        // Update playtime and sync timer on client
        saveState.setPlaytime(saveState.getPlaytime() + (time - saveState.getPlaytimeUpdate()));
        saveState.setPlaytimeUpdate(time);
        player.sendActionBar(Component.text("Playtime: " + (saveState.getPlaytime() / 1000.0) + "s")); //todo formatting
    }

}
