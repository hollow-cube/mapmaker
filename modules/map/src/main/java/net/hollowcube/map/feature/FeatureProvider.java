package net.hollowcube.map.feature;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.config.ConfigProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface FeatureProvider {

    /**
     * Global feature init when a server starts.
     */
    default @NotNull ListenableFuture<Void> init(@NotNull ConfigProvider config) {
        return Futures.immediateVoidFuture();
    }

    /**
     * Global feature shutdown when a server stops.
     */
    default void shutdown() {
    }

    /**
     * Called when a map is being initialized. Implementation should register any events or
     * other implementation details to make the feature functional.
     * <p>
     * A map will not be considered "ready" until this feature completes.
     *
     * @param world The {@link MapWorld} being initialized.
     * @return A null future indicates that a feature is not enabled, so a cleanup callback will not be sent
     *         A non-null future indicates that a feature is enabled, and the map will wait for the future before
     *         being considered ready for players.
     */
    default @Nullable ListenableFuture<Void> initMap(@NotNull MapWorld world) {
        return null;
    }

    /**
     * Called when a map is being cleaned up. Implementation should unregister any events or
     * other implementation details used in the map.
     * <p>
     * Note that all players will be gone by the time this is called.
     * {@link net.hollowcube.map.event.MapWorldPlayerStopPlayingEvent} should be used for player cleanup.
     *
     * @param world The {@link MapWorld} being cleaned up
     * @return A future which completes when the map is cleaned up.
     */
    default @NotNull ListenableFuture<Void> cleanupMap(@NotNull MapWorld world) {
        return Futures.immediateVoidFuture();
    }

}
