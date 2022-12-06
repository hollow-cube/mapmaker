package net.hollowcube.map.event;

import net.hollowcube.map.event.trait.MapWorldEvent;
import net.hollowcube.map.world.MapWorld;
import org.jetbrains.annotations.NotNull;

public record MapWorldRegisterEvent(
        @NotNull MapWorld mapWorld
) implements MapWorldEvent {

    @Override
    public @NotNull MapWorld getMapWorld() {
        return mapWorld;
    }

}
