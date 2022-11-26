package net.hollowcube.map;

import net.hollowcube.map.command.HubCommand;
import net.hollowcube.map.event.PlayerSpawnInInstanceEvent;
import net.hollowcube.map.instance.MapInstance;
import net.hollowcube.mapmaker.map.MapManager;
import net.hollowcube.mapmaker.model.MapData;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Wrapper for managing maps.
 *
 * Will eventually act as the api for interacting with other hub and map nodes. Will need some refactoring
 */
public class MapServer implements MapManager {
    public static final Tag<Boolean> MAP_MARKER = Tag.Boolean("mapmaker:map_marker");

    private final EventNode<Event> eventNode = EventNode.all("mapmaker:map");

    public MapServer() {
        MinecraftServer.getGlobalEventHandler().addChild(eventNode);
        eventNode.addListener(PlayerSpawnEvent.class, this::handleSpawn);
        eventNode.addListener(PlayerSpawnEvent.class, PlayerSpawnInInstanceEvent::handler);

        var commandManager = MinecraftServer.getCommandManager();
        commandManager.register(new HubCommand());
    }

    @Override
    public @NotNull CompletableFuture<Void> joinMap(@NotNull MapData map, int flags, @NotNull Player player) {
        return MapInstance.create(map, flags)
                .thenCompose(instance -> {
                    MinecraftServer.getInstanceManager().registerInstance(instance);
                    return player.setInstance(instance, new Pos(0.5, 60, 0.5));
                });
    }

    private void handleSpawn(@NotNull PlayerSpawnEvent event) {
        // Spawn event is not an InstanceEvent, so we need to filter it.
        if (!event.getSpawnInstance().hasTag(MapInstance.MAP_ID))
            return;

        var player = event.getPlayer();
        player.refreshCommands();
    }

}
