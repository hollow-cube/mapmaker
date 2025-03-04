package net.hollowcube.compat.axiom.events;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import org.jetbrains.annotations.NotNull;

public interface AxiomEvent extends PlayerInstanceEvent {

    @NotNull Player player();

    @Override
    default @NotNull Player getPlayer() {
        return player();
    }
}
