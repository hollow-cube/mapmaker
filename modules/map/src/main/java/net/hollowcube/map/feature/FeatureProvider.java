package net.hollowcube.map.feature;

import net.hollowcube.common.config.ConfigProvider;
import net.hollowcube.map.world.MapWorld;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

public interface FeatureProvider {

    /**
     * Global feature init when a server starts.
     */
    default void init(@NotNull ConfigProvider config) {
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
     * A map will not be considered "ready" until this future completes.
     *
     * @param world The {@link MapWorld} being initialized.
     * @return False indicates that a feature is not enabled, so a cleanup callback will not be sent
     */
    default @Blocking boolean initMap(@NotNull MapWorld world) {
        return false;
    }

    /**
     * Called when a map is being cleaned up. Implementation should unregister any events or
     * other implementation details used in the map.
     * <p>
     * Note that all players will be gone by the time this is called.
     * {@link net.hollowcube.map.event.MapWorldPlayerStopPlayingEvent} should be used for player cleanup.
     *
     * @param world The {@link MapWorld} being cleaned up
     */
    default @Blocking void cleanupMap(@NotNull MapWorld world) {
    }

}
