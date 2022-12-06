package net.hollowcube.map.world;

import net.hollowcube.map.event.MapWorldUnregisterEvent;
import net.hollowcube.util.FutureUtil;
import net.hollowcube.world.WorldManager;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.util.StaticAbuse;
import net.hollowcube.mapmaker.util.TagUtil;
import net.hollowcube.world.event.PlayerInstanceLeaveEvent;
import net.hollowcube.world.event.PlayerSpawnInInstanceEvent;
import net.hollowcube.world.BaseWorld;
import net.hollowcube.world.generation.MapGenerators;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class MapWorld extends BaseWorld {

    public static final Tag<String> MAP_ID = Tag.String("mapmaker:map/id");
    public static final Tag<MapData> MAP_DATA = TagUtil.noop("mapmaker:map/data");

    private static final Tag<MapWorld> THIS_TAG = TagUtil.noop("mapmaker:map/world");

    public static @NotNull MapWorld fromInstance(@NotNull Instance instance) {
        return Objects.requireNonNull(instance.getTag(THIS_TAG));
    }

    public static final int FLAG_NONE = 0;
    public static final int FLAG_EDIT = 1;

    private final MapData map;
    private final int flags;

    public MapWorld(@NotNull WorldManager worldManager, @NotNull MapData map, int flags) {
        super(worldManager, map.getId());
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
        player.setGameMode(GameMode.CREATIVE);

        player.sendMessage("Now editing " + map.getName());
    }

    private void initPlayerForPlaying(@NotNull PlayerSpawnInInstanceEvent event) {
        var player = event.getPlayer();
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlying(true);

        player.sendMessage("Now playing " + map.getName());
    }

    private void preventBlockBreak(PlayerBlockBreakEvent event) {
        event.setCancelled(true);
    }

    private void preventBlockPlace(PlayerBlockPlaceEvent event) {
        event.setCancelled(true);
    }

    private void handlePlayerLeave(@NotNull PlayerInstanceLeaveEvent event) {
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
