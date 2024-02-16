package net.hollowcube.mapmaker.bridge;

import net.hollowcube.map.runtime.ServerBridge;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

/**
 * Implements required communication from map servers back to the hub server.
 * <p>
 * This represents the only form of direct communication from map servers to hubs. The implementation may
 * communicate in any way. For example the dev server uses a singleton class as a bridge within the same process
 * however an alternative implementation may use a proxy, a REST API, asynchronous messaging, etc.
 */
public interface MapToHubBridge extends ServerBridge {

    /**
     * Sends the given player back to a hub instance.
     * The chosen hub is implementation dependent and should not be used.
     * <p>
     * The {@link Player} should be considered invalid after this call,
     * until/unless there is an exception
     */
    @Blocking
    void sendPlayerToHub(@NotNull Player player);

}
