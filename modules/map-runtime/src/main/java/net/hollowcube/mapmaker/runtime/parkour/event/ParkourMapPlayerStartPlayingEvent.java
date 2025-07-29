package net.hollowcube.mapmaker.runtime.parkour.event;

import net.hollowcube.mapmaker.map.event.Map2PlayerEvent;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld2;
import net.minestom.server.entity.Player;

/// Called when a player starts playing a parkour map. Can be used to initialize state for the player.
///
/// @param isFreshState True if this initialization is the first for a new save state (first join or reset, including reset before first checkpoint)
/// @param isMapJoin    True if the player just joined the world. Not necessarily the first time playing the map and not called on reset.
public record ParkourMapPlayerStartPlayingEvent(
        ParkourMapWorld2 world, Player player,
        boolean isFreshState, boolean isMapJoin
) implements Map2PlayerEvent {

    public boolean showsGameplayWarnings() {
        return isMapJoin;
    }
}
