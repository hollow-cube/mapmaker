package net.hollowcube.mapmaker.map.event;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.event.trait.MapWorldEvent;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Called whenever the player could potentially need a state on itself updated from a new state.
 * This can be called when the player is initialized, reset, or when the player's state is updated.
 * @param mapWorld
 * @param player
 */
public record MapPlayerUpdateStateEvent(
        @NotNull MapWorld mapWorld,
        @NotNull Player player
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
