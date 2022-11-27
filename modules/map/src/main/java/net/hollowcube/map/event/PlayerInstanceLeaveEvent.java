package net.hollowcube.map.event;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Triggered when a player leaves an instance
 */
public record PlayerInstanceLeaveEvent(
        @NotNull Player player
) implements PlayerInstanceEvent {

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }

}
