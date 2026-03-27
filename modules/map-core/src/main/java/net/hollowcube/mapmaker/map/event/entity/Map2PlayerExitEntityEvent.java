package net.hollowcube.mapmaker.map.event.entity;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.event.trait.Map2PlayerEvent;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;

public record Map2PlayerExitEntityEvent(
        MapWorld world, Player player,
        Entity exitedEntity
) implements Map2PlayerEvent {
}
