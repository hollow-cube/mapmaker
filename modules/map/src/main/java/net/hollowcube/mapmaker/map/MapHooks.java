package net.hollowcube.mapmaker.map;

import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;

/**
 * Utility functions for interacting with map worlds.
 */
public final class MapHooks {
    //todo not in love with this class, would like to split its duties into better places.
    private MapHooks() {
    }

    /**
     * If set, indicates that the player is bound for a map, and contains a future that will be completed when
     * the map is ready to join.
     */

    /**
     * Associated player is used to mark an entity as "associated" with a player. For now that is used for
     * the entity to reach a finish plate and trigger the event for the player.
     */
    public static final Tag<Player> ASSOCIATED_PLAYER = Tag.Transient("mapmaker:map/associated_player");


}
