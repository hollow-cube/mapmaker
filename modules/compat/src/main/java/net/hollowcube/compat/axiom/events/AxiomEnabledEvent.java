package net.hollowcube.compat.axiom.events;

import net.minestom.server.entity.Player;

public record AxiomEnabledEvent(
    Player player,
    boolean isEnabled
) implements AxiomEvent {
}

