package net.hollowcube.mapmaker.map.event.vnext;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.event.trait.MapWorldEvent;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public record MapSpectatorToggleFlightEvent(
        @NotNull MapWorld mapWorld,
        @NotNull Player player,
        boolean isFlying
) implements MapWorldEvent {
    @Override
    public @NotNull MapWorld getMapWorld() {
        return mapWorld;
    }
}
