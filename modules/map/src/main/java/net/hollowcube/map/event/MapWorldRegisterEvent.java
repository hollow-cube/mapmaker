package net.hollowcube.map.event;

import net.hollowcube.map.event.trait.MapWorldEvent;
import net.hollowcube.map.world.MapWorldNew;
import org.jetbrains.annotations.NotNull;

public record MapWorldRegisterEvent(
        @NotNull MapWorldNew mapWorld
) implements MapWorldEvent {

    @Override
    public @NotNull MapWorldNew getMapWorld() {
        return mapWorld;
    }

}
