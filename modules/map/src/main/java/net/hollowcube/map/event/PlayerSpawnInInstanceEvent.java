package net.hollowcube.map.event;

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

    public static void handler(@NotNull PlayerSpawnEvent event) {
        EventDispatcher.call(new PlayerSpawnInInstanceEvent(event.getPlayer()));
    }

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }

}
