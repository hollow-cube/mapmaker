package net.hollowcube.world.event;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Like PlayerSpawnEvent, but implements InstanceEvent.
 */
public record PlayerSpawnInInstanceEvent(
        @NotNull Player player
) implements PlayerInstanceEvent {

    static {
        // Register a handler for this event globally, always.
        MinecraftServer.getGlobalEventHandler().addListener(PlayerSpawnEvent.class, event -> {
            EventDispatcher.call(new PlayerSpawnInInstanceEvent(event.getPlayer()));
        });
    }

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }

}
