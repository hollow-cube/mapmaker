package net.hollowcube.map;

import net.hollowcube.map.command.HubCommand;
import net.hollowcube.map.event.MapWorldUnregisterEvent;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.map.MapManager;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.world.WorldManager;
import net.hollowcube.world.event.PlayerSpawnInInstanceEvent;
import net.hollowcube.world.storage.FileStorageS3;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerSpawnEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wrapper for managing maps.
 *
 * Will eventually act as the api for interacting with other hub and map nodes. Will need some refactoring
 */
public class MapServer implements MapManager {
    public static final Logger logger = LoggerFactory.getLogger(MapServer.class);

    private final EventNode<Event> eventNode = EventNode.all("mapmaker:map");
    private final WorldManager worldManager = new WorldManager(FileStorageS3.connect(
            "http://localhost:9000/",
            "DTprdE3DBZ7vG8wQ",
            "qByxgkPV7rO7zo12KmRUkikSBMwYJCRj"
    ));


    // map id -> flags -> world
    // Used to send players to the same world if there is already one instance of it.
    private final Map<String, Map<Integer, MapWorld>> maps = new ConcurrentHashMap<>();

    public MapServer() {
        MinecraftServer.getGlobalEventHandler().addChild(eventNode);
        eventNode.addListener(PlayerSpawnEvent.class, this::handleSpawn);
        eventNode.addListener(MapWorldUnregisterEvent.class, this::handleMapUnregister);

        var commandManager = MinecraftServer.getCommandManager();
        commandManager.register(new HubCommand());
    }

    public @NotNull WorldManager worldManager() {
        return worldManager;
    }

    @Override
    public @NotNull CompletableFuture<Void> joinMap(@NotNull MapData map, int flags, @NotNull Player player) {
        var activeMaps = maps.computeIfAbsent(map.getId(), id -> new ConcurrentHashMap<>());

        // Search for a world with the same flags
        var activeWorld = activeMaps.get(flags);
        if (activeWorld != null) {
            return player.setInstance(activeWorld.instance(), new Pos(0.5, 60, 0.5));
        }

        // No such map, create a new one
        var world = new MapWorld(worldManager, map, flags);
        activeMaps.put(flags, world);

        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
        if (map.getMapFileId() != null)
            future = world.loadWorld();
        return future.thenCompose(unused -> player.setInstance(world.instance(), new Pos(0.5, 60, 0.5)));
    }

    private void handleSpawn(@NotNull PlayerSpawnEvent event) {
        // Spawn event is not an InstanceEvent, so we need to filter it.
        if (!event.getSpawnInstance().hasTag(MapWorld.MAP_ID))
            return;

        var player = event.getPlayer();
        player.refreshCommands();
    }

    private void handleMapUnregister(@NotNull MapWorldUnregisterEvent event) {
        var activeMaps = maps.get(event.getMap().getId());
        if (activeMaps == null) {
            // Something went wrong and the instance is not registered
            logger.error("Attempted to unregister {}, but it was not registered.", event.getMap().getId());
            return;
        }

        var removed = activeMaps.remove(event.mapWorld().flags());
        if (removed == null) {
            // Something went wrong and the instance is not registered
            logger.error("Attempted to unregister {}, but it was not registered.", event.getMap().getId());
        }
    }

}
