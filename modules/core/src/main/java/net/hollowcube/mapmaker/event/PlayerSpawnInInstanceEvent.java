package net.hollowcube.mapmaker.event;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;

/**
 * Like PlayerSpawnEvent, but implements InstanceEvent.
 */
public record PlayerSpawnInInstanceEvent(
    Player player,
    boolean isFirstSpawn
) implements PlayerInstanceEvent {

    static {
        // Register a handler for this event globally, always.
        MinecraftServer.getGlobalEventHandler().addListener(PlayerSpawnEvent.class, event -> {
            EventDispatcher.call(new PlayerSpawnInInstanceEvent(event.getPlayer(), event.isFirstSpawn()));
        });
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    public boolean isFirstSpawn() {
        return isFirstSpawn;
    }

}
