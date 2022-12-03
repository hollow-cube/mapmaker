package net.hollowcube.map.world;

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
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class MapWorld extends BaseWorld {
    public static final Tag<String> MAP_ID = Tag.String("mapmaker:map/id");
    public static final Tag<MapData> MAP_DATA = TagUtil.noop("mapmaker:map/data");

    private final MapData map;

    public MapWorld(@NotNull WorldManager worldManager, @NotNull MapData map) {
        super(worldManager, map.getId());
        this.map = map;

        instance().getWorldBorder().setDiameter(100); //todo
        instance().setGenerator(MapGenerators.flatWorld());

        instance().setTag(MAP_ID, map.getId());
        instance().setTag(MAP_DATA, map);

        var eventNode = instance().eventNode();
        //todo support playing a map, not just editing.
        eventNode.addListener(PlayerSpawnInInstanceEvent.class, this::initPlayerForEditing);

        // Handle the last person leaving
        eventNode.addListener(PlayerInstanceLeaveEvent.class, this::handlePlayerLeave);
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

    private void initPlayerForEditing(@NotNull PlayerSpawnInInstanceEvent event) {
        var player = event.getPlayer();
        player.setGameMode(GameMode.CREATIVE);

        player.sendMessage("Now editing " + map.getName());
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

        // No more players, save the world
        saveAndUnloadWorld()
                .thenRun(() -> System.out.println("Saved and unloaded world " + map.getId()))
                .exceptionally(FutureUtil::handleException);
    }
}
