package net.hollowcube.compat.axiom.events;

import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public record AxiomEnabledEvent(
        @NotNull Player player,
        boolean isEnabled
) implements AxiomEvent {

}

