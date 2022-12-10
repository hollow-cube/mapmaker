package net.hollowcube.mapmaker.hub;

import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * todo: this should be removed in favor of a local bid interface on both sides.
 */
public interface HubManager {

    @NotNull CompletableFuture<Void> sendToHub(@NotNull Player player);

    class TemporaryIAmTerrible {
        public static HubManager INSTANCE;
    }
}
