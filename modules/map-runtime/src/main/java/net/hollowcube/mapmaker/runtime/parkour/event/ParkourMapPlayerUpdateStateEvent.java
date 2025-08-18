package net.hollowcube.mapmaker.runtime.parkour.event;

import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.map.event.trait.Map2PlayerEvent;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.minestom.server.entity.Player;

public record ParkourMapPlayerUpdateStateEvent(
        ParkourMapWorld world, Player player,
        SaveState saveState, PlayState playState,
        boolean isFreshState, boolean isMapJoin,
        boolean isReset
) implements Map2PlayerEvent {
}
