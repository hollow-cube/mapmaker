package net.hollowcube.map2.event;

import net.hollowcube.map2.MapWorld;
import net.hollowcube.map2.event.trait.MapWorldEvent;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public record MapPlayerInitEvent(
        @NotNull MapWorld mapWorld,
        @NotNull Player player,
        boolean isFirstInit
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
