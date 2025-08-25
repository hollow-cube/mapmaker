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

        // isReset is gross and exists exclusively because of global map settings.
        // We use an empty updatestateevent to reset the player's state but for
        // map settings which are global that would still apply it. Gross.
        // When we remove global map settings we can remove this.
        boolean isReset
) implements Map2PlayerEvent {
}
