package net.hollowcube.mapmaker.map.event.vnext;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.event.trait.MapWorldEvent;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Note: currently this event is only used to trigger a reset and is not always called when a player is being reset.
 *
 * @param player
 * @param mapWorld
 * @param toCheckpoint
 */
public record MapPlayerResetEvent(
        @NotNull Player player,
        @NotNull MapWorld mapWorld,
        boolean toCheckpoint
) implements PlayerEvent, MapWorldEvent {

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }

    @Override
    public @NotNull MapWorld getMapWorld() {
        return mapWorld;
    }

}
