package net.hollowcube.map.world;

import net.hollowcube.common.result.FutureResult;
import net.hollowcube.common.util.ExtraTags;
import net.hollowcube.map.MapHooks;
import net.hollowcube.map.MapServer;
import net.hollowcube.map.event.MapWorldRegisterEvent;
import net.hollowcube.map.event.MapWorldUnregisterEvent;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.model.PlayerData;
import net.hollowcube.util.FutureUtil;
import net.hollowcube.world.BaseWorld;
import net.hollowcube.world.event.PlayerInstanceLeaveEvent;
import net.hollowcube.world.event.PlayerSpawnInInstanceEvent;
import net.hollowcube.world.generation.MapGenerators;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public abstract class MapWorld extends BaseWorld {
    private static final Logger logger = LoggerFactory.getLogger(MapWorld.class);

    public static final Tag<String> MAP_ID = Tag.String("mapmaker:map/id");
    public static final Tag<MapData> MAP_DATA = ExtraTags.Transient("mapmaker:map/data");

    private static final Tag<MapWorld> THIS_TAG = ExtraTags.Transient("mapmaker:map/world");

    public static @NotNull MapWorld fromInstance(@NotNull Instance instance) {
        return Objects.requireNonNull(instance.getTag(THIS_TAG));
    }

    public static final int FLAG_EDIT = 1; //todo remove me

    protected MapServer mapServer;
    protected final MapData map;

    public MapWorld(@NotNull MapServer mapServer, @NotNull MapData map) {
        super(mapServer.worldManager(), map.getId());
        this.mapServer = mapServer;
        this.map = map;

        instance().getWorldBorder().setDiameter(100); //todo
        instance().setGenerator(MapGenerators.flatWorld());

        instance().setTag(THIS_TAG, this);
        instance().setTag(MAP_ID, map.getId());
        instance().setTag(MAP_DATA, map);

        var eventNode = instance().eventNode();
        eventNode.addListener(PlayerSpawnInInstanceEvent.class, this::initPlayer);
        eventNode.addListener(PlayerInstanceLeaveEvent.class, this::handlePlayerLeave);

        // Mark the map as registered
        EventDispatcher.call(new MapWorldRegisterEvent(this));
    }

    public @NotNull MapData map() {
        return map;
    }

    /**
     * Called when a player joins the map.
     * <p>
     * The player is held at the map spawn with a loading indicator until the future returns.
     * <p>
     * todo hold the player duing loading
     */
    protected abstract @NotNull FutureResult<Void> initPlayer(@NotNull Player player);

    /**
     * Called to save the player. If `remove` is set, the player is leaving the map and all state should be cleared as well
     */
    protected abstract @NotNull FutureResult<Void> savePlayer(@NotNull Player player, boolean remove);

    /**
     * Called to close this world, whatever that means for the world type (eg save the world for editing)
     */
    protected abstract @NotNull FutureResult<Void> closeWorld();


    // Implementation

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
                    return mapServer.mapStorage()
                            .updateMap(map)
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

    private void initPlayer(@NotNull PlayerSpawnInInstanceEvent event) {
        var player = event.getPlayer();

        // Teleport the player to spawn and show loading.
        player.teleport(map.getSpawnPoint()).exceptionally(FutureUtil::handleException);
        player.showTitle(Title.title(Component.text("Loading..."), Component.text(""),
                Title.Times.times(Duration.ofSeconds(0), Duration.ofSeconds(10000), Duration.ofSeconds(0))));
        //todo while loading they should not be able to do anything.

        initPlayer(player)
                .then(unused -> {
                    // Stop loading
                    player.clearTitle();
                })
                .thenErr(err -> {
                    //todo probably should not kick in the future
                    logger.error("Failed to load map for player {}: {}", player.getUsername(), err);
                    player.kick(Component.text("Failed to load map: " + err.message()));
                });
    }

    private void handlePlayerLeave(@NotNull PlayerInstanceLeaveEvent event) {
        // Save the player who is leaving
        var player = event.getPlayer();
        player.removeTag(MapHooks.PLAYING);
        savePlayer(player, true)
                .thenErr(err -> {
                    //todo should maybe hold the player for this or convey it somehow.
                    // maybe tell the proxy to tell the player
                    logger.error("Failed to save save state for player {} in map {}: {}",
                            PlayerData.fromPlayer(player).getId(), map.getId(), err);
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
        instance().scheduleNextTick(unused -> closeWorld());
    }

}
