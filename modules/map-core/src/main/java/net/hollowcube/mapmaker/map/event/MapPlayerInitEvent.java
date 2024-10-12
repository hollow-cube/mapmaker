package net.hollowcube.mapmaker.map.event;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.event.trait.MapWorldEvent;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public record MapPlayerInitEvent(
        @NotNull MapWorld mapWorld,
        @NotNull Player player,
        // This is false on reset/checkpoint, but true on full map reset
        boolean isFirstInit,
        // This is only true the very first time the player joins the map,
        // whether or not they have a prior state.
        boolean isMapJoin
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
