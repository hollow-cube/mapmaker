package net.hollowcube.map.world;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import net.hollowcube.common.result.FutureResult;
import net.hollowcube.common.util.ExtraTags;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.map.MapHooks;
import net.hollowcube.map.MapServer;
import net.hollowcube.map.event.*;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.item.ItemRegistry;
import net.hollowcube.map.util.StringUtil;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.model.PlayerData;
import net.hollowcube.mapmaker.model.SaveState;
import net.hollowcube.mapmaker.storage.SaveStateStorage;
import net.hollowcube.world.BaseWorld;
import net.hollowcube.world.dimension.DimensionTypes;
import net.hollowcube.world.event.PlayerInstanceLeaveEvent;
import net.hollowcube.world.event.PlayerSpawnInInstanceEvent;
import net.hollowcube.world.generation.MapGenerators;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings({"UnstableApiUsage", "PointlessBitwiseExpression"})
public abstract class MapWorld extends BaseWorld {

    /*

    Base MapWorld
    PlayingMapWorld
    EditingMapWorld
    TestingMapWorld

    Testing worlds use the same underlying instance as the editing world, but are tagged as playing and features registered independently.

     */

    private static final Logger logger = LoggerFactory.getLogger(MapWorld.class);

    public static final Tag<String> MAP_ID = Tag.String("mapmaker:map/id");
    public static final Tag<MapData> MAP_DATA = ExtraTags.Transient("mapmaker:map/data");

    private static final Tag<MapWorld> THIS_TAG = ExtraTags.Transient("mapmaker:map/world");

    public static @NotNull MapWorld fromInstance(@NotNull Instance instance) {
        return Objects.requireNonNull(instance.getTag(THIS_TAG));
    }

    public static @Nullable MapWorld optionalFromInstance(@Nullable Instance instance) {
        if (instance == null) return null;
        return instance.getTag(THIS_TAG);
    }

    public static final int FLAG_EDITING = 1 << 0;
    public static final int FLAG_PLAYING = 1 << 1;
    public static final int FLAG_TESTING = 1 << 2;

    private final EventNode<InstanceEvent> scopedNode = EventNode.type("mapmaker:map/scoped", EventFilter.INSTANCE);
    private final List<FeatureProvider> enabledFeatures = new ArrayList<>();
    private final ItemRegistry itemRegistry = new ItemRegistry();

    protected final MapServer mapServer;
    protected final MapData map;
    protected int flags = 0;

    protected MapWorld(@NotNull MapServer mapServer, @NotNull MapData map) {
        super(mapServer.worldManager(), map.getId(), new InstanceContainer(StringUtil.seededUUID(map.getId()), DimensionTypes.FULL_BRIGHT));
        this.mapServer = mapServer;
        this.map = map;

        instance().setGenerator(MapGenerators.voidWorld());

        instance().setTag(THIS_TAG, this);
        instance().setTag(MAP_ID, map.getId());
        instance().setTag(MAP_DATA, map);

        var eventNode = instance().eventNode();
        eventNode.addChild(scopedNode);
        eventNode.addChild(itemRegistry.eventNode());
        eventNode.addListener(PlayerSpawnInInstanceEvent.class, this::handlePlayerSpawn);
        eventNode.addListener(PlayerInstanceLeaveEvent.class, this::handlePlayerLeave);

        for (var feature : mapServer.features()) {
            feature.initMap(this);
        }

        // Mark the map as registered
        EventDispatcher.call(new MapWorldRegisterEvent(this));
    }

    public @NotNull ListenableFuture<Void> initFeatures() {
        var futures = new ArrayList<ListenableFuture<Void>>();
        for (var feature : mapServer.features()) {
            var future = feature.initMap(this);
            if (future == null) continue;

            futures.add(future);
            enabledFeatures.add(feature);
        }

        return Futures.whenAllComplete(futures).call(() -> null, Runnable::run);
    }

    public @NotNull ItemRegistry itemRegistry() {
        return itemRegistry;
    }

    public @NotNull MapData map() {
        return map;
    }

    public @NotNull MapServer server() {
        return mapServer;
    }

    public int flags() {
        return flags;
    }

    protected void initSaveState(@NotNull SaveState saveState) {
    }

    protected void initPlayerFromSaveState(@NotNull Player player, @NotNull SaveState saveState) {
    }

    protected abstract void initHotbar(@NotNull Player player);

    /**
     * Called to save the player. If `remove` is set, the player is leaving the map and all state should be cleared as well
     */
    protected void updateSaveStateForPlayer(@NotNull Player player, @NotNull SaveState saveState, boolean remove) {
    }

    /**
     * Called to close this world, whatever that means for the world type (eg save the world for editing)
     */
    protected abstract @NotNull FutureResult<Void> closeWorld();

    // Feature utilities

    /**
     * Adds an eventNode which will be removed when the world is unloaded.
     */
    public void addScopedEventNode(@NotNull EventNode<InstanceEvent> eventNode) {
        scopedNode.addChild(eventNode);
    }


    // Implementation

    @Override
    public @NotNull CompletableFuture<Void> loadWorld() {
        return super.loadWorld()
                .thenAccept(unused -> EventDispatcher.call(new MapWorldLoadEvent(this)));
    }

    @Override
    public @NotNull CompletableFuture<@NotNull String> saveWorld() {
        //todo handle failure here (probably write to disk and keep trying to save or write somewhere else or something)
        return super.saveWorld()
                .thenCompose(fileId -> {
                    map.setMapFileId(fileId);
                    return mapServer.mapStorage()
                            .updateMap(map)
                            .toCompletableFuture()
                            // Still need to return the file id.
                            .thenApply(unused -> fileId);
                });
    }

    @Override
    public @NotNull CompletableFuture<Void> unloadWorld() {
        EventDispatcher.call(new MapWorldUnloadEvent(this));
        for (var child : Set.copyOf(scopedNode.getChildren())) {
            scopedNode.removeChild(child);
        }

        var featureCleanup = Futures.whenAllComplete(enabledFeatures.stream()
                .map(feature -> feature.cleanupMap(this))
                .toList()).call(() -> null, Runnable::run);

        return FutureUtil.wrap(featureCleanup)
                .thenCompose(unused -> super.unloadWorld())
                .thenRun(() -> EventDispatcher.call(new MapWorldUnregisterEvent(this)));
    }

    private void handlePlayerSpawn(@NotNull PlayerSpawnInInstanceEvent event) {
        var player = event.getPlayer();
        player.getInventory().clear();
        initHotbar(player);

        // Teleport the player to spawn and show loading.
        player.teleport(map.getSpawnPoint()).exceptionally(FutureUtil::handleException);
        player.showTitle(Title.title(Component.text("Loading..."), Component.text(""),
                Title.Times.times(Duration.ofSeconds(0), Duration.ofSeconds(10000), Duration.ofSeconds(0))));
        //todo while loading they should not be able to do anything.

        var playerId = PlayerData.fromPlayer(player).getId();
        var saveStates = mapServer.saveStateStorage();
        FluentFuture.from(saveStates.getLatestSaveState(playerId, map.getId()))
                .catchingAsync(SaveStateStorage.NotFoundError.class, unused -> {
                    // Create savestate if not exists
                    var saveState = new SaveState();
                    saveState.setId(UUID.randomUUID().toString());
                    saveState.setPlayerId(playerId);
                    saveState.setMapId(map.getId());
                    saveState.setStartTime(Instant.now());
                    saveState.setPos(map.getSpawnPoint());
                    initSaveState(saveState);
                    return saveStates.createSaveState(saveState);
                }, Runnable::run)
                .transform(saveState -> {
                    player.setTag(MapHooks.PLAYING, true);
                    player.setTag(SaveState.TAG, saveState);

                    initPlayerFromSaveState(player, saveState);

                    EventDispatcher.call(new MapWorldPlayerStartPlayingEvent(this, player));
                    return null;
                }, Runnable::run)
                .catching(Throwable.class, err -> {
                    logger.error("Failed to load save state for player {} in map {}: {}", playerId, map.getId(), err);
                    player.kick("failed to load save state: " + err.getMessage());
                    return null;
                }, Runnable::run)
                .addCallback(new FutureCallback<>() {
                    @Override
                    public void onSuccess(Object result) {
                        // Stop loading
                        player.clearTitle();
                    }

                    @Override
                    public void onFailure(@NotNull Throwable t) {
                        //todo probably should not kick in the future
                        logger.error("Failed to load map for player {}", player.getUsername(), t);
                        player.kick(Component.text("Failed to load map: " + t.getMessage()));
                    }
                }, Runnable::run);
    }

    private void handlePlayerLeave(@NotNull PlayerInstanceLeaveEvent event) {
        // Save the player who is leaving
        var player = event.getPlayer();
        var saveState = SaveState.optionalFromPlayer(player);
        if (saveState != null) {

            updateSaveStateForPlayer(player, saveState, true);
            EventDispatcher.call(new MapWorldPlayerStopPlayingEvent(this, player));
//        player.tagHandler().updateContent(new NBTCompound()); // Clear the player tag

            var saveStateStorage = mapServer.saveStateStorage();
            Futures.addCallback(saveStateStorage.updateSaveState(saveState), new FutureCallback<>() {
                @Override
                public void onSuccess(Void result) {
                }

                @Override
                public void onFailure(@NotNull Throwable t) {
                    //todo should maybe hold the player for this or convey it somehow.
                    // maybe tell the proxy to tell the player
                    logger.error("Failed to save save state for player {} in map {}: {}",
                            PlayerData.fromPlayer(player).getId(), map.getId(), t);
                }
            }, Runnable::run);

        }

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
        instance().scheduleNextTick(unused -> closeWorld());
    }

}
