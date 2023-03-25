package net.hollowcube.map.feature2;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.config.ConfigProvider;
import org.jetbrains.annotations.NotNull;

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
     * @return A future which completes when this feature is initialized.
     */
    default @NotNull ListenableFuture<Void> initMap(@NotNull MapWorld world) {
        return Futures.immediateVoidFuture();
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
