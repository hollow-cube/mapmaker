package net.hollowcube.mapmaker.runtime.parkour.event;

import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.map.event.trait.Map2PlayerEvent;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.minestom.server.entity.Player;

public record ParkourMapPlayerTookActionEvent(
    ParkourMapWorld world,
    Player player,
    SaveState saveState
) implements Map2PlayerEvent {
}
