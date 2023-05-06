package net.hollowcube.mapmaker.event;

import net.minestom.server.event.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a map is deleted.
 *
 * @param mapId The ID of the map that was deleted
 */
public record MapDeletedEvent(@NotNull String mapId) implements Event {
}
