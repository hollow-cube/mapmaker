package net.hollowcube.mapmaker.map.event.entity;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.hollowcube.mapmaker.map.event.trait.MapWorldEvent;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

public class MarkerEntityExitedEvent implements PlayerInstanceEvent, MapWorldEvent {
    private final MapWorld world;
    private final Player player;
    private final MarkerEntity marker;

    public MarkerEntityExitedEvent(@NotNull MapWorld world, @NotNull Player player, @NotNull MarkerEntity marker) {
        this.world = world;
        this.player = player;
        this.marker = marker;
    }

    public @NotNull MarkerEntity getMarkerEntity() {
        return marker;
    }

    @Override
    public @NotNull MapWorld getMapWorld() {
        return world;
    }

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }

    @Override
    public @NotNull Instance getInstance() {
        return world.instance();
    }
}
