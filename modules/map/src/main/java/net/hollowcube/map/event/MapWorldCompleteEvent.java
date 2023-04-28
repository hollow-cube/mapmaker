package net.hollowcube.map.event;

import net.hollowcube.map.event.trait.MapWorldEvent;
import net.hollowcube.map.world.MapWorldNew;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Triggered when a player completes a map.
 */
public record MapWorldCompleteEvent(
        @NotNull MapWorldNew mapWorld,
        @NotNull Player player
) implements MapWorldEvent {

    @Override
    public @NotNull MapWorldNew getMapWorld() {
        return mapWorld;
    }

    public @NotNull Player getPlayer() {
        return player;
    }

}
