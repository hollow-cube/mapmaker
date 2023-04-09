package net.hollowcube.map.world;

import net.hollowcube.common.result.FutureResult;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.map.MapHooks;
import net.hollowcube.map.MapServer;
import net.hollowcube.map.event.MapWorldCompleteEvent;
import net.hollowcube.map.gui.CompletedMapView;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.model.PlayerData;
import net.hollowcube.mapmaker.model.SaveState;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("UnstableApiUsage")
public class PlayingMapWorld extends MapWorld {
    private static final Logger logger = LoggerFactory.getLogger(PlayingMapWorld.class);

    public PlayingMapWorld(@NotNull MapServer mapServer, @NotNull MapData map) {
        super(mapServer, map);

        var eventNode = instance().eventNode();
        eventNode.addListener(PlayerBlockBreakEvent.class, this::preventBlockBreak); //todo again, BaseWorld settings
        eventNode.addListener(PlayerBlockPlaceEvent.class, this::preventBlockPlace); //todo again, BaseWorld settings
        eventNode.addListener(MapWorldCompleteEvent.class, this::handleMapCompletion);

        if (MapHooks.isCompletable(map)) {
            eventNode.addListener(InstanceTickEvent.class, this::tickPlayers);
        }
    }

    @Override
    protected void initPlayerFromSaveState(@NotNull Player player, @NotNull SaveState saveState) {
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlying(true);

        // If exact pos is saved, teleport there. Otherwise teleport to latest checkpoint or map spawn point
        if (saveState.getPos() != null) {
            player.teleport(saveState.getPos()).exceptionally(FutureUtil::handleException);
        } else if (saveState.getCheckpoint() != null) {
            var checkpoint = map.getPoi(saveState.getCheckpoint()); //todo handle null case
            player.teleport(new Pos(checkpoint.getPos())).exceptionally(FutureUtil::handleException);
        } else {
            player.teleport(map.getSpawnPoint()).exceptionally(FutureUtil::handleException);
        }
        saveState.setPlaytimeUpdate(System.currentTimeMillis()); // Start timer now

        player.sendMessage("Now playing " + map.getName());
    }

    @Override
    protected void updateSaveStateForPlayer(@NotNull Player player, @NotNull SaveState saveState, boolean remove) {
        // Update relevant fields
        //todo should be based on a map setting
//        saveState.setPos(player.getPosition());
    }

    @Override
    protected @NotNull FutureResult<Void> closeWorld() {
        return FutureResult.wrap(unloadWorld())
                //todo handle error better.
                .thenErr(err -> logger.error("Failed to unload world: {}", err));
    }

    /**
     * Ticks each player, updating their playtime action bar
     */
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
        FutureResult.wrap(mapServer.saveStateStorage().updateSaveState(saveState))
                .thenErr(err -> logger.error("failed to save save state for player {}: {}", playerData.getId(), err));

        server().newOpenGUI(player, CompletedMapView::new);
    }

    private void updatePlayer(@NotNull Player player, long time) {
        var saveState = SaveState.fromPlayer(player);

        // Update playtime and sync timer on client
        saveState.setPlaytime(saveState.getPlaytime() + (time - saveState.getPlaytimeUpdate()));
        saveState.setPlaytimeUpdate(time);
        player.sendActionBar(Component.text("Playtime: " + (saveState.getPlaytime() / 1000.0) + "s")); //todo formatting
    }

}
