package net.hollowcube.mapmaker.punishments.event;

import net.hollowcube.mapmaker.punishments.types.Punishment;
import net.minestom.server.event.Event;

public record PunishmentRevokedEvent(
    Punishment punishment
) implements Event {
}
