package net.hollowcube.mapmaker.map.event;

import net.hollowcube.mapmaker.map.MapPlayer;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.event.trait.Map2PlayerEvent;
import net.minestom.server.entity.Player;

public record PlayerLandEvent(MapWorld world, MapPlayer player) implements Map2PlayerEvent {
    @Override
    public Player getPlayer() {
        return player;
    }
}
