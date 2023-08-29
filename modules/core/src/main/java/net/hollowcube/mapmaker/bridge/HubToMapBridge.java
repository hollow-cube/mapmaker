package net.hollowcube.mapmaker.bridge;

import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

/**
 * Implements required communication from hub servers to all available map servers.
 * <p>
 * This represents the only form of direct communication from hub servers to maps. The implementation may
 * communicate in any way. For example the dev server uses a singleton class as a bridge within the same process
 * however an alternative implementation may use a proxy, a REST API, asynchronous messaging, etc.
 */
public interface HubToMapBridge {

    /**
     * Sends the given player to an instance of the given map.
     * <p>
     * Note: This is a very temporary method which is only valid for a dev server where there is a single map and hub
     * both within the same process. This method will be removed in the future to support something like a bid
     * system, or an external allocator/matchmaker.
     */
    @Blocking
    void joinMap(@NotNull Player player, @NotNull String mapId, @NotNull JoinMapState joinMapState);


    public enum JoinMapState {
        EDITING,
        PLAYING,
        SPECTATING
    }
}
