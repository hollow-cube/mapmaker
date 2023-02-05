package net.hollowcube.map.event;

import net.hollowcube.map.event.trait.MapWorldEvent;
import net.hollowcube.map.world.MapWorld;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a world loads in a map. ONLY called if there is a saved world.
 * @param mapWorld
 */
public record MapWorldLoadEvent(
        @NotNull MapWorld mapWorld
) implements MapWorldEvent {

    @Override
    public @NotNull MapWorld getMapWorld() {
        return mapWorld;
    }

}
