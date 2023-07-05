package net.hollowcube.map.world;

import net.hollowcube.map.MapHooks;
import net.hollowcube.map.MapServer;
import net.hollowcube.map.event.MapPlayerInitEvent;
import net.hollowcube.map.event.MapWorldPlayerStopPlayingEvent;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.item.ItemRegistry;
import net.hollowcube.mapmaker.instance.MapInstance;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.SaveStateUpdateRequest;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.instance.generation.MapGenerators;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PlayingMapWorld implements InternalMapWorld {
    private static final System.Logger logger = System.getLogger(EditingMapWorld.class.getName());

    // If set, indicates that the player is an editor.
    private static final Tag<Boolean> TAG_PLAYING = Tag.Boolean("mapworld/playing").defaultValue(false);

    private final MapServer server;
    private final MapData map;
    private int flags = 0;

    private final MapInstance instance;
    private final Set<Player> activePlayers = Collections.synchronizedSet(new HashSet<>());
    private final Set<Player> spectatingPlayers = Collections.synchronizedSet(new HashSet<>());

    private final List<FeatureProvider> enabledFeatures = new ArrayList<>();
    private final ItemRegistry itemRegistry;
    private final EventNode<InstanceEvent> scopedNode = EventNode.event("mapworld/playing", EventFilter.INSTANCE, ev -> {
        if (ev instanceof PlayerEvent event) {
            return event.getPlayer().hasTag(TAG_PLAYING);
        }
        return true;
    });

    public PlayingMapWorld(@NotNull MapServer server, @NotNull MapData map) {
        this.server = server;
        this.map = map;
        this.flags |= FLAG_PLAYING;

        instance = new MapInstance(getDimensionName());
        instance.setGenerator(MapGenerators.voidWorld());
        instance.setTag(SELF_TAG, this);

        var eventNode = instance.eventNode();
        this.itemRegistry = new ItemRegistry();
        eventNode.addChild(itemRegistry.eventNode());
        eventNode.addChild(scopedNode);

        eventNode.addListener(PlayerBlockBreakEvent.class, this::preventBlockBreak); //todo move to some utility
        eventNode.addListener(PlayerBlockPlaceEvent.class, this::preventBlockPlace); //todo move to some utility
    }

    @Override
    public @NotNull MapServer server() {
        return server;
    }

    @Override
    public @NotNull MapData map() {
        return map;
    }

    @Override
    public int flags() {
        return flags;
    }

    @Override
    public @NotNull ItemRegistry itemRegistry() {
        return itemRegistry;
    }

    @Override
    public void addScopedEventNode(@NotNull EventNode<InstanceEvent> eventNode) {
        this.scopedNode.addChild(eventNode);
    }


    @Override
    public @NotNull Point spawnPoint() {
        return map.settings().getSpawnPoint();
    }

    @Override
    public @NotNull Instance instance() {
        return instance;
    }

    @Override
    public @Blocking void load() {
        // Load the map itself (eg blocks, if present)
        var mapData = server().mapService().getMapWorld(map.id(), true);
        if (mapData != null) {
            instance.load(mapData);
        }

        this.enabledFeatures.addAll(MapWorldHelpers.loadFeatures(this));
    }

    @Override
    public @Blocking void close() {
        instance.unload();
    }

    @Override
    public @Nullable MapWorld getMapForPlayer(@NotNull Player player) {
        return activePlayers.contains(player) || spectatingPlayers.contains(player) ? this : null;
    }

    @Override
    public @Blocking void acceptPlayer(@NotNull Player player) {
        var playerData = PlayerDataV2.fromPlayer(player);

        var saveState = MapWorldHelpers.getOrCreateSaveState(this, playerData.id());

        activePlayers.add(player);
        player.setTag(TAG_PLAYING, true);
        player.setTag(MapHooks.PLAYING, true); // Legacy
        player.setTag(SaveState.TAG, saveState);
        player.refreshCommands();

        player.getInventory().clear();
        player.setGameMode(GameMode.ADVENTURE);
        var pos = Objects.requireNonNullElse(saveState.pos(), map.settings().getSpawnPoint());
        player.teleport(pos).join();

        EventDispatcher.call(new MapPlayerInitEvent(this, player, true));
        player.sendMessage("Now playing " + map.settings().getName());
        saveState.setPlayStartTime(System.currentTimeMillis());
    }

    public @Blocking void startSpectating(@NotNull Player player, boolean teleport) {
        spectatingPlayers.add(player);
        player.setGameMode(GameMode.SPECTATOR);
        if (teleport) player.teleport(map.settings().getSpawnPoint()).join();
        player.sendMessage("Now spectating " + map.settings().getName());
    }

    @Override
    public @Blocking void removePlayer(@NotNull Player player) {
        EventDispatcher.call(new MapWorldPlayerStopPlayingEvent(this, player));

        player.removeTag(TAG_PLAYING);
        player.removeTag(MapHooks.PLAYING); // Legacy
        activePlayers.remove(player);
        spectatingPlayers.remove(player);

        var saveState = SaveState.optionalFromPlayer(player);
        if (saveState == null) return;

        saveState.updatePlaytime();

        var update = new SaveStateUpdateRequest();
        update.setPlaytime(saveState.getPlaytime());
        update.setCompleted(saveState.isCompleted());

        try {
            var playerData = PlayerDataV2.fromPlayer(player);
            server.mapService().updateSaveState(map.id(), playerData.id(), saveState.id(), update);
            logger.log(System.Logger.Level.INFO, "Updated savestate for {0}", player.getUuid());
        } catch (Exception e) {
            logger.log(System.Logger.Level.ERROR, "Failed to save player state for {0}", player.getUuid(), e);
        }
    }

    private void preventBlockBreak(PlayerBlockBreakEvent event) {
        event.setCancelled(true);
    }

    private void preventBlockPlace(PlayerBlockPlaceEvent event) {
        event.setCancelled(true);
    }

    private @NotNull String getDimensionName() {
        return String.format("mapmaker:map/%s/p", map.id().substring(0, 8));
    }

}
