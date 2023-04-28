package net.hollowcube.map.event;

import net.hollowcube.map.event.trait.MapWorldEvent;
import net.hollowcube.map.world.MapWorldNew;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a world loads in a map. ONLY called if there is a saved world.
 */
public record MapWorldUnloadEvent(
        @NotNull MapWorldNew mapWorld
) implements MapWorldEvent {

    @Override
    public @NotNull MapWorldNew getMapWorld() {
        return mapWorld;
    }

}
