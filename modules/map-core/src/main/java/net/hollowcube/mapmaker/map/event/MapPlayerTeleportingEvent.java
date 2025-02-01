package net.hollowcube.mapmaker.map.event;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.event.trait.MapWorldEvent;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a player is about to teleport to a different location in a map.
 */
public record MapPlayerTeleportingEvent(
        @NotNull MapWorld mapWorld,
        @NotNull Player player,
        @NotNull Point destination
) implements MapWorldEvent, PlayerEvent {
    @Override
    public @NotNull MapWorld getMapWorld() {
        return mapWorld;
    }

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }
}
