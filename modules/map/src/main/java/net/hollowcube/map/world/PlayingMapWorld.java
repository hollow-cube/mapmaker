package net.hollowcube.map.world;

import net.hollowcube.common.util.FontUtil;
import net.hollowcube.map.MapHooks;
import net.hollowcube.map.MapServer;
import net.hollowcube.map.biome.BiomeContainer;
import net.hollowcube.map.event.MapPlayerInitEvent;
import net.hollowcube.map.event.MapPlayerStartFinishedEvent;
import net.hollowcube.map.event.MapPlayerStartSpectatorEvent;
import net.hollowcube.map.event.MapWorldPlayerStopPlayingEvent;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.item.ItemRegistry;
import net.hollowcube.map.world.polar.ReadWorldAccess;
import net.hollowcube.mapmaker.instance.MapInstance;
import net.hollowcube.mapmaker.instance.generation.MapGenerators;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapVerification;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.to_be_refactored.ActionBar;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.hollowcube.mapmaker.to_be_refactored.FontUIBuilder;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSwapItemEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class PlayingMapWorld implements InternalMapWorld {
    private static final Logger logger = LoggerFactory.getLogger(EditingMapWorld.class);

    private static final BadSprite SPECTATOR_SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/spectator"));
    private static final BadSprite FINISHED_SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/finished"));

    // If set, indicates that the player is an editor.
    public static final Tag<Boolean> TAG_PLAYING = Tag.Boolean("mapworld/playing").defaultValue(false);

    private final MapServer server;
    private final MapData map;
    private int flags = 0;

    private final MapInstance instance;
    private final Set<Player> activePlayers = Collections.synchronizedSet(new HashSet<>());
    private final Set<Player> spectatingPlayers = Collections.synchronizedSet(new HashSet<>());

    private final List<FeatureProvider> enabledFeatures = new ArrayList<>();
    private final ItemRegistry itemRegistry;
    private final BiomeContainer biomeContainer;
    private final EventNode<InstanceEvent> scopedNode = EventNode.event("mapworld/playing", EventFilter.INSTANCE, ev -> {
        if (ev instanceof PlayerEvent event) {
            return event.getPlayer().hasTag(TAG_PLAYING);
        }
        return true;
    });

    private final ActionBar.Provider spectatingActionBarProvider = this::buildSpectatorWidget;
    private final ActionBar.Provider finishedActionBarProvider = this::buildFinishedWidget;

    public PlayingMapWorld(@NotNull MapServer server, @NotNull MapData map) {
        this.server = server;
        this.map = map;
        this.flags |= FLAG_PLAYING;
        if (map.verification() == MapVerification.PENDING) {
            this.flags |= FLAG_VERIFYING;
        }

        instance = new MapInstance(getDimensionName());
        instance.setGenerator(MapGenerators.voidWorld());
        instance.setTag(SELF_TAG, this);

        var eventNode = instance.eventNode();

        this.itemRegistry = new ItemRegistry();
        eventNode.addChild(itemRegistry.eventNode());

        this.biomeContainer = new BiomeContainer();

        eventNode.addChild(scopedNode);

        //todo move the following to some utility
        eventNode.addListener(PlayerBlockBreakEvent.class, event -> event.setCancelled(true));
        eventNode.addListener(PlayerBlockPlaceEvent.class, event -> event.setCancelled(true));
        eventNode.addListener(PlayerSwapItemEvent.class, event -> event.setCancelled(true));
        eventNode.addListener(InventoryPreClickEvent.class, event -> event.setCancelled(true));
        eventNode.addListener(ItemDropEvent.class, event -> event.setCancelled(true));

        instance.scheduler().buildTask(this::updateSpectators)
                .repeat(TaskSchedule.seconds(1))
                .schedule();
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
    public @NotNull BiomeContainer biomes() {
        return biomeContainer;
    }

    @Override
    public void addScopedEventNode(@NotNull EventNode<InstanceEvent> eventNode) {
        this.scopedNode.addChild(eventNode);
    }

    @Override
    public @NotNull Pos spawnPoint() {
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
            instance.load(mapData, new ReadWorldAccess(this));
        }

        this.biomeContainer.init();
        this.enabledFeatures.addAll(MapWorldHelpers.loadFeatures(this));
    }

    @Override
    public @Blocking void close(boolean shutdown) {
        logger.info("Closing playing world {}", map.id());
        var kickMessage = Component.translatable("mapmaker.shutdown");
        for (var player : Set.copyOf(activePlayers)) {
            removePlayer(player, true);
            if (shutdown) {
                EventDispatcher.call(new PlayerDisconnectEvent(player)); // todo why isnt this done by Minestom
                player.kick(kickMessage);
            }
        }
        for (var player : Set.copyOf(spectatingPlayers)) {
            removePlayer(player);
            if (shutdown) player.kick(kickMessage);
        }
        instance.unload();
    }

    @Override
    public @Nullable MapWorld getMapForPlayer(@NotNull Player player) {
        return activePlayers.contains(player) || spectatingPlayers.contains(player) ? this : null;
    }

    @Override
    public @Blocking void acceptPlayer(@NotNull Player player, boolean firstSpawn) {
        var playerData = PlayerDataV2.fromPlayer(player);

        var saveState = MapWorldHelpers.getOrCreateSaveState(this, playerData.id());
        player.setTag(SaveState.TAG, saveState);

        var pos = Objects.requireNonNullElse(saveState.pos(), map.settings().getSpawnPoint());
        player.teleport(pos).join(); //todo should probably be done from elsewhere because it depends on checkpoints

        activePlayers.add(player);
        player.setTag(TAG_PLAYING, true);
        player.setTag(MapHooks.PLAYING, true); // Legacy

        MapWorldHelpers.resetPlayer(player);

        EventDispatcher.call(new MapPlayerInitEvent(this, player, firstSpawn));
        if (saveState.getPlaytime() > 0) {
            // If the playtime is non-zero (ie they have played before) start timing immediately.
            // Otherwise, we will start timing when they move the first time.
            saveState.setPlayStartTime(System.currentTimeMillis());
        }
    }

    public @Blocking void startSpectating(@NotNull Player player, boolean teleport) {
        player.setTag(TAG_PLAYING, false); // Sanity
        player.setTag(MapHooks.PLAYING, false); // Sanity

        spectatingPlayers.add(player);

        MapWorldHelpers.resetPlayer(player);
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlying(true);
        player.setInvisible(true);
        ActionBar.forPlayer(player).addProvider(spectatingActionBarProvider);

        instance.eventNode().call(new MapPlayerStartSpectatorEvent(this, player));

        if (teleport) player.teleport(map.settings().getSpawnPoint()).join();
//        player.sendMessage("Now spectating " + map.settings().getName());
    }

    public @Blocking void startFinished(@NotNull Player player, boolean teleport) {

        player.setTag(TAG_PLAYING, false); // Sanity
        player.setTag(MapHooks.PLAYING, false); // Sanity

        spectatingPlayers.add(player);

        MapWorldHelpers.resetPlayer(player);
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlying(true);
        player.setInvisible(true);
        ActionBar.forPlayer(player).addProvider(finishedActionBarProvider);

        instance.eventNode().call(new MapPlayerStartFinishedEvent(this, player));

        if (teleport) player.teleport(map.settings().getSpawnPoint()).join();
//        player.sendMessage("Now spectating " + map.settings().getName());
    }

    public @Blocking boolean isSpectating(@NotNull Player player) {
        return spectatingPlayers.contains(player);
    }

    @Override
    public @Blocking void removePlayer(@NotNull Player player) {
        removePlayer(player, true);
    }

    public @Blocking void removePlayer(@NotNull Player player, boolean save) {
        EventDispatcher.call(new MapWorldPlayerStopPlayingEvent(this, player));

        player.removeTag(TAG_PLAYING);
        player.removeTag(MapHooks.PLAYING);
        activePlayers.remove(player);
        spectatingPlayers.remove(player);
        ActionBar.forPlayer(player).removeProvider(spectatingActionBarProvider);
        ActionBar.forPlayer(player).removeProvider(finishedActionBarProvider);

        MapWorldHelpers.resetPlayer(player);

        if (save) {
            var saveState = SaveState.optionalFromPlayer(player);
            if (saveState == null) return;

            saveState.updatePlaytime();
            saveState.setPos(player.getPosition());

            try {
                var update = saveState.getUpdateRequest();

                var playerData = PlayerDataV2.fromPlayer(player);
                server.mapService().updateSaveState(map.id(), playerData.id(), saveState.id(), update);

                logger.info("Updated savestate for {}", player.getUuid());
            } catch (Exception e) {
                logger.error("Failed to save player state for {}", player.getUuid(), e);
            }
        }
        player.removeTag(SaveState.TAG);
    }

    private @NotNull String getDimensionName() {
        return String.format("mapmaker:map/%s/p", map.id().substring(0, 8));
    }

    @Override
    public @NotNull Set<Player> players() {
        return Set.copyOf(activePlayers);
    }

    private void buildSpectatorWidget(@NotNull Player player, @NotNull FontUIBuilder builder) {
        builder.pushColor(FontUtil.NO_SHADOW);
        builder.pos(-SPECTATOR_SPRITE.width() / 2).drawInPlace(SPECTATOR_SPRITE);
    }

    private void buildFinishedWidget(@NotNull Player player, @NotNull FontUIBuilder builder) {
        builder.pushColor(FontUtil.NO_SHADOW);
        builder.pos(-FINISHED_SPRITE.width() / 2).drawInPlace(FINISHED_SPRITE);
    }

    private void updateSpectators() {
        for (var player : spectatingPlayers) {
            player.setInvisible(true);
        }
    }
}
