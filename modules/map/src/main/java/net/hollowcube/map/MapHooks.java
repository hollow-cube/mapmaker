package net.hollowcube.map;

import net.hollowcube.map.world.MapWorld;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Utility functions for interacting with map worlds.
 */
public final class MapHooks {
    private MapHooks() {}

    @ApiStatus.Internal
    public static final Tag<Boolean> PLAYING = Tag.Boolean("mapmaker:map/playing").defaultValue(false);


    /** Returns true if the given player is currently playing. */
    public static boolean isPlayerPlaying(@NotNull Player player) {
        return player.getTag(PLAYING);
    }

    /** Returns true if the given player is inside a map world at all, whether it be editing, playing, spectating, etc. */
    public static boolean isPlayerInMap(@NotNull Player player) {
        var instance = player.getInstance();
        if (instance == null) return false;
        return instance.hasTag(MapWorld.MAP_ID);
    }


}
