package net.hollowcube.mapmaker.map.event;

import net.hollowcube.mapmaker.map.MapPlayer;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.event.trait.Map2PlayerEvent;

public record MapPlayerJoinEvent(MapWorld world, MapPlayer player) implements Map2PlayerEvent {
}
