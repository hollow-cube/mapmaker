package net.hollowcube.compat.axiom.events;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerInstanceEvent;

public interface AxiomEvent extends PlayerInstanceEvent {

    Player player();

    @Override
    default Player getPlayer() {
        return player();
    }
}
