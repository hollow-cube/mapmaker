package net.hollowcube.map;

import net.hollowcube.map.command.HubCommand;
import net.hollowcube.map.event.PlayerSpawnInInstanceEvent;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.map.MapManager;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.world.WorldManager;
import net.hollowcube.world.storage.FileStorageS3;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerSpawnEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Wrapper for managing maps.
 *
 * Will eventually act as the api for interacting with other hub and map nodes. Will need some refactoring
 */
public class MapServer implements MapManager {

    private final EventNode<Event> eventNode = EventNode.all("mapmaker:map");
    private final WorldManager worldManager = new WorldManager(FileStorageS3.connect(
            "http://localhost:9000/",
            "DTprdE3DBZ7vG8wQ",
            "qByxgkPV7rO7zo12KmRUkikSBMwYJCRj"
    ));

    public MapServer() {
        MinecraftServer.getGlobalEventHandler().addChild(eventNode);
        eventNode.addListener(PlayerSpawnEvent.class, this::handleSpawn);
        eventNode.addListener(PlayerSpawnEvent.class, PlayerSpawnInInstanceEvent::handler);

        var commandManager = MinecraftServer.getCommandManager();
        commandManager.register(new HubCommand());
    }

    @Override
    public @NotNull CompletableFuture<Void> joinMap(@NotNull MapData map, int flags, @NotNull Player player) {
        var world = new MapWorld(worldManager, map);
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

}
