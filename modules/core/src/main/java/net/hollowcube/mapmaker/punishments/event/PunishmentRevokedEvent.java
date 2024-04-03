package net.hollowcube.mapmaker.punishments.event;

import net.hollowcube.mapmaker.punishments.types.Punishment;
import net.minestom.server.event.Event;
import org.jetbrains.annotations.NotNull;

public record PunishmentRevokedEvent(
        @NotNull Punishment punishment
) implements Event {
}
