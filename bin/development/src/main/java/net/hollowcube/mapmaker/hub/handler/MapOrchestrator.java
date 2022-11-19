package net.hollowcube.mapmaker.hub.handler;

import net.hollowcube.mapmaker.hub.MapHandle;
import net.hollowcube.mapmaker.model.MapData;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Responsible for handling map instantiation and joining, either locally or on other servers.
 */
public interface MapOrchestrator {

    @NotNull CompletableFuture<MapHandle> openMap(@NotNull MapData map, int flags);

    /**
     * Sends the given player to the given map. After calling this method, the player should be considered invalid.
     */
    @NotNull CompletableFuture<Void> joinMap(@NotNull Player player, @NotNull MapHandle handle);
}
