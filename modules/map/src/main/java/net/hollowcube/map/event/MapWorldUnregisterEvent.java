package net.hollowcube.map.event;

import net.hollowcube.map.event.trait.MapWorldEvent;
import net.hollowcube.map.world.MapWorldNew;
import org.jetbrains.annotations.NotNull;

//todo there should be a `WorldUnloadEvent` in the instance module
public record MapWorldUnregisterEvent(
        @NotNull MapWorldNew mapWorld
) implements MapWorldEvent {

    @Override
    public @NotNull MapWorldNew getMapWorld() {
        return mapWorld;
    }

}
