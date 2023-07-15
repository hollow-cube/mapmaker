package net.hollowcube.map;

import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Utility functions for interacting with map worlds.
 */
public final class MapHooks {
    //todo not in love with this class, would like to split its duties into better places.
    private MapHooks() {
    }

    /**
     * Associated player is used to mark an entity as "associated" with a player. For now that is used for
     * the entity to reach a finish plate and trigger the event for the player.
     */
    public static final Tag<Player> ASSOCIATED_PLAYER = Tag.Transient("mapmaker:map/associated_player");

    @ApiStatus.Internal
    public static final Tag<Boolean> PLAYING = Tag.Boolean("mapmaker:map/playing").defaultValue(false);


    /**
     * Returns true if the given player is currently playing.
     */
    public static boolean isPlayerPlaying(@NotNull Player player) {
        return player.getTag(PLAYING);
    }

    // Implicit tags

//    public static boolean isCompletable(@NotNull MapData map) {
//        return map.getPois().stream().anyMatch(poi -> poi.getType().equals(FinishPlateBlock.POI_TYPE));
//    }


}
