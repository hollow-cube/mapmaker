package net.hollowcube.mapmaker.map.feature;

import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.event.MapWorldPlayerStopPlayingEvent;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.instance.block.BlockHandler;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

public interface FeatureProvider {

    /**
     * Global feature init when a server starts.
     */
    default void init(@NotNull ConfigLoaderV3 config) {
    }

    /**
     * Global feature shutdown when a server stops.
     */
    default void shutdown() {
    }

    default @NotNull List<Supplier<BlockHandler>> blockHandlers() {
        return List.of();
    }

    default void preinitMap(@NotNull MapWorld world) {
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
     * {@link MapWorldPlayerStopPlayingEvent} should be used for player cleanup.
     *
     * @param world The {@link MapWorld} being cleaned up
     */
    default @Blocking void cleanupMap(@NotNull MapWorld world) {
    }

    /**
     * Called when a player joins a map world. This is called during the {@link AsyncPlayerConfigurationEvent}
     *
     * @param world The {@link MapWorld} the player is joining
     * @param player The {@link Player} that is joining the map world
     */
    default @Blocking void configurePlayer(@NotNull MapWorld world, @NotNull Player player) {
    }
}
