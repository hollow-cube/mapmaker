package net.hollowcube.mapmaker.map.event;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.event.trait.Map2PlayerEvent;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a player is about to teleport to a different location in a map.
 */
public record MapPlayerTeleportingEvent(
        @NotNull MapWorld world,
        @NotNull Player player,
        @NotNull Point destination
) implements Map2PlayerEvent {

}
