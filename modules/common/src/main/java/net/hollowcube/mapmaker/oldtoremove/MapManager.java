package net.hollowcube.mapmaker.oldtoremove;

import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.result.FutureResult;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Handles loading and saving maps, as well as associated locking mechanisms (todo).
 * todo: this should be removed in favor of a local bid interface on both sides.
 */
public interface MapManager {

//    /**
//     * "Open"s a map. When the future is completed, it is possible for a player to join the map.
//     * <p>
//     * Implementations may decide what it means to "open" a map. For example, read it from disk,
//     * or from a distributed filesystem, return an existing instance, etc.
//     */
//    @NotNull CompletableFuture<@NotNull MapHandle> openMap(@NotNull MapData map, int flags);

    /**
     * Sends the given player to the given map.
     * <p>
     * After calling this method, the player should be considered invalid unless the future fails.
     */
    @NotNull FutureResult<Void> joinMap(@NotNull MapData map, int flags, @NotNull Player player);

}
