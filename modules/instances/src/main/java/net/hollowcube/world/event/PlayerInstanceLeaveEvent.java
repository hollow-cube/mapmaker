package net.hollowcube.world.event;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

/**
 * Triggered when a player leaves an instance
 */
public record PlayerInstanceLeaveEvent(
        @NotNull Player player,
        @NotNull Instance instance
) implements PlayerInstanceEvent {

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }

    @Override
    public @NotNull Instance getInstance() {
        return instance;
    }

}
