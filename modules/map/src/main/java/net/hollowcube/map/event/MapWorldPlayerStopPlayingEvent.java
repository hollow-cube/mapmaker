package net.hollowcube.map.event;

import net.hollowcube.map.event.trait.MapWorldEvent;
import net.hollowcube.map.world.MapWorldNew;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public record MapWorldPlayerStopPlayingEvent(@NotNull MapWorldNew mapWorld, @NotNull Player player) implements MapWorldEvent, PlayerEvent {
    @Override
    public @NotNull MapWorldNew getMapWorld() {
        return mapWorld;
    }

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }
}
