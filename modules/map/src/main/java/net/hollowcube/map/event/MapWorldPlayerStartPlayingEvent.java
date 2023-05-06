package net.hollowcube.map.event;

import net.hollowcube.map.event.trait.MapWorldEvent;
import net.hollowcube.map.world.MapWorld;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Called after a player has loaded (eg savestate is present) into a map and is starting playing.
 */
public record MapWorldPlayerStartPlayingEvent(@NotNull MapWorld mapWorld,
                                              @NotNull Player player) implements MapWorldEvent, PlayerEvent {
    @Override
    public @NotNull MapWorld getMapWorld() {
        return mapWorld;
    }

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }
}
