package net.hollowcube.map.event;

import net.hollowcube.map.event.trait.MapWorldEvent;
import net.hollowcube.map.world.MapWorld;
import org.jetbrains.annotations.NotNull;

//todo there should be a `WorldUnloadEvent` in the instance module
public record MapWorldUnregisterEvent(
        @NotNull MapWorld mapWorld
) implements MapWorldEvent {

    @Override
    public @NotNull MapWorld getMapWorld() {
        return mapWorld;
    }

}
