package net.hollowcube.mapmaker.event;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.instance.Instance;

/**
 * Triggered when a player leaves an instance
 */
public record PlayerInstanceLeaveEvent(
    Player player,
    Instance instance
) implements PlayerInstanceEvent {

    @Override
    public Player getPlayer() {
        return player;
    }

    @Override
    public Instance getInstance() {
        return instance;
    }

}
