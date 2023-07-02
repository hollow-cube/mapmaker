package net.hollowcube.map.event;

import net.hollowcube.map.event.trait.MapWorldEvent;
import net.hollowcube.map.world.MapWorld;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Called to trigger a player reset, for example when they fall out of the world, stop sprinting in only sprint, etc.
 */
public record MapPlayerResetTriggerEvent(
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
