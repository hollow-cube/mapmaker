package net.hollowcube.mapmaker.map.event.vnext;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.event.trait.MapWorldEvent;
import net.minestom.server.coordinate.Pos;
import org.jetbrains.annotations.NotNull;

public record MapChangeSpawnPointEvent(@NotNull MapWorld world, @NotNull Pos newSpawnPoint) implements MapWorldEvent {
    @Override
    public @NotNull MapWorld getMapWorld() {
        return world;
    }
}
