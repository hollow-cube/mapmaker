package net.hollowcube.mapmaker.runtime.parkour.event;

import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.map.event.Map2PlayerEvent;
import net.hollowcube.mapmaker.map.world.savestate.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld2;
import net.minestom.server.entity.Player;

public record ParkourMapPlayerUpdateStateEvent(
        ParkourMapWorld2 world, Player player,
        SaveState saveState, PlayState playState
) implements Map2PlayerEvent {
}
