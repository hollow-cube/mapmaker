package net.hollowcube.map.world;

import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map2.AbstractMapWorld;
import net.hollowcube.map2.MapServer;
import net.hollowcube.map2.polar.ReadWorldAccess;
import net.hollowcube.map2.util.MapWorldHelpers;
import net.hollowcube.mapmaker.instance.MapInstance;
import net.hollowcube.mapmaker.instance.generation.MapGenerators;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.map.SaveStateUpdateResponse;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerSwapItemEvent;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class PlayingMapWorld extends AbstractMapWorld {
    private final Logger logger = LoggerFactory.getLogger(PlayingMapWorld.class);

    private final EventNode<InstanceEvent> eventNode = EventNode.type("playing-events", EventFilter.INSTANCE)
            .addListener(PlayerBlockBreakEvent.class, event -> event.setCancelled(true))
            .addListener(PlayerBlockPlaceEvent.class, event -> event.setCancelled(true))
            .addListener(PlayerSwapItemEvent.class, event -> event.setCancelled(true))
            .addListener(InventoryPreClickEvent.class, event -> event.setCancelled(true))
            .addListener(ItemDropEvent.class, event -> event.setCancelled(true));

    private final List<FeatureProvider> enabledFeatures = new ArrayList<>();

    public PlayingMapWorld(@NotNull MapServer server, @NotNull MapData map) {
        super(server, map, new MapInstance(map.createDimensionName('p')));
        instance.setGenerator(MapGenerators.voidWorld());

        instance.eventNode().addChild(eventNode); // Needs spectators, so register on instance.
    }

    @Override
    protected void load() {
        // Load the map itself (eg blocks, if present)
        var mapData = server().mapService().getMapWorld(map().id(), true);
        if (mapData != null) {
            instance.load(mapData, new ReadWorldAccess(this));
        }

        this.biomes().init();
        this.enabledFeatures.addAll(MapWorldHelpers.loadFeatures(this));
    }

    @Override
    protected void close() {
        super.close(); // Remove players & spectators
        instance.unload();
    }

    @Override
    public void addPlayer(@NotNull Player player) {
        var playerData = PlayerDataV2.fromPlayer(player);

        var saveState = MapWorldHelpers.getOrCreateSaveState(this, playerData.id());
        player.setTag(SaveState.TAG, saveState);

        var pos = saveState.playState().pos().orElse(map().settings().getSpawnPoint());
        player.teleport(pos).join();

        super.addPlayer(player); // Add to player list & reset inventory.

//        EventDispatcher.call(new MapPlayerInitEvent(this, player, firstSpawn)); todo
        if (saveState.getPlaytime() > 0) {
            // If the playtime is non-zero (ie they have played before) start timing immediately.
            // Otherwise, we will start timing when they move the first time.
            saveState.setPlayStartTime(System.currentTimeMillis());
        }
    }

    @Override
    public void addSpectator(@NotNull Player player) {
        super.addSpectator(player); // Add to spectator list & reset inventory.

        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlying(true);
        player.setInvisible(true);


//        ActionBar.forPlayer(player).addProvider(spectatingActionBarProvider);
//        instance.eventNode().call(new MapPlayerStartSpectatorEvent(this, player));
//        if (teleport) player.teleport(map.settings().getSpawnPoint()).join();
    }

//    public @Blocking void startFinished(@NotNull Player player, boolean teleport) {
//
//        player.setTag(TAG_PLAYING, false); // Sanity
//        player.setTag(MapHooks.PLAYING, false); // Sanity
//
//        spectatingPlayers.add(player);
//
//        net.hollowcube.map.world.MapWorldHelpers.resetPlayer(player);
//        player.setGameMode(GameMode.ADVENTURE);
//        player.setAllowFlying(true);
//        player.setInvisible(true);
//        ActionBar.forPlayer(player).addProvider(finishedActionBarProvider);
//
//        instance.eventNode().call(new MapPlayerStartFinishedEvent(this, player));
//
//        if (teleport) player.teleport(map.settings().getSpawnPoint()).join();
////        player.sendMessage("Now spectating " + map.settings().getName());
//    }

    @Override
    public void removePlayer(@NotNull Player player) {
        if (isPlaying(player)) removeActivePlayer(player);
        else if (isSpectating(player)) removeSpectatingPlayer(player);
    }

    public @Nullable SaveStateUpdateResponse removeActivePlayer(@NotNull Player player) {
        if (!isPlaying(player)) return null;

        //         EventDispatcher.call(new MapWorldPlayerStopPlayingEvent(this, player));
        super.removePlayer(player); // Remove from player list

        // Update the playtime and playing state to the current state
        var saveState = SaveState.optionalFromPlayer(player);
        if (saveState == null) return null; // Sanity check
        saveState.updatePlaytime();
        saveState.playState().setPos(player.getPosition());
        var update = saveState.createUpdateRequest();

        // Write the save state to the database
        try {
            var playerData = PlayerDataV2.fromPlayer(player);
            var resp = server().mapService().updateSaveState(map().id(), playerData.id(), saveState.id(), update);
            logger.info("Updated savestate for {}", player.getUuid());
            return resp;
        } catch (Exception e) {
            logger.error("Failed to save player state for {}", player.getUuid(), e);
            return null;
        } finally {
            player.removeTag(SaveState.TAG);
        }
    }

    private void removeSpectatingPlayer(@NotNull Player player) {
        if (!isSpectating(player)) return;

        super.removePlayer(player); // Remove from player list & reset

        // ActionBar.forPlayer(player).removeProvider(spectatingActionBarProvider);
        //        ActionBar.forPlayer(player).removeProvider(finishedActionBarProvider);
    }
}
