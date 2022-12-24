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
    private MapHooks() {}

    @ApiStatus.Internal
    public static final Tag<Boolean> PLAYING = Tag.Boolean("mapmaker:map/playing").defaultValue(false);


    /** Returns true if the given player is currently playing. */
    public static boolean isPlayerPlaying(@NotNull Player player) {
        return player.getTag(PLAYING);
    }


}
