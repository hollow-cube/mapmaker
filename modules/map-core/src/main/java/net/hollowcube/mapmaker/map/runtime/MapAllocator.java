package net.hollowcube.mapmaker.map.runtime;

import net.hollowcube.mapmaker.map.AbstractMapWorld;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapServer;
import net.hollowcube.mapmaker.map.MapWorld;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Future;


public interface MapAllocator {

    static @NotNull MapAllocator direct(@NotNull MapServer server) {
        return new DirectMapAllocator(server);
    }

    // Low level/direct api

    /**
     * Directly creates a map world, bypassing expected lifecycle and capacity checks. Upon return, the map will
     * be ready to accept players and will be considered "active" by the server.
     *
     * <p>The map returned by this function will never be removed automatically, even if it has zero players.</p>
     *
     * @param map       The map data to create a world for
     * @param worldType The type of world to create
     * @param <T>       The type of world to create
     * @return The created world
     */
    @Blocking <T extends AbstractMapWorld> @NotNull T allocateDirect(@NotNull MapData map, @NotNull Class<T> worldType);

    /**
     * Closes the given world immediately, attempting to send all players to the hub and kicking them if unable.
     *
     * <p>It is valid to call this function on a {@link #allocateDirect(MapData, Class)} world to close it.</p>
     *
     * @param world The world to close.
     */
    @Blocking
    void free(@NotNull MapWorld world, @Nullable Component reason);

    // Higher level API

    /**
     * Creates a map of the given type if it doesn't exist, or returns the existing one if it does.
     *
     * <p>Capacity rules will be enforced, and the created map will be removed when it has no players.</p>
     *
     * @param map       The map data to create a world for
     * @param worldType The type of world to create
     * @return A future holding the world, or null if it could not be created
     */
    @NonBlocking
    <T extends AbstractMapWorld> @NotNull Future<@Nullable T> create(@NotNull MapData map, @NotNull Class<T> worldType);

    /**
     * Frees a world by its {@link MapWorld#worldId()} if and only if it is owned by this allocator.
     * Basically the safer version of {@link #free(MapWorld, Component)}.
     *
     * @param worldId The world ID to free
     * @return True if it was freed, false otherwise (not found or error)
     */
    @NonBlocking
    @NotNull Future<Boolean> destroy(@NotNull String worldId, @NotNull Component reason);

    /**
     * Closes all maps with the given ID, attempting to send all players to the hub and kicking them if unable.
     *
     * <p>Note that this will NOT close maps created with {@link #allocateDirect(MapData, Class)}.</p>
     *
     * @param mapId The ID of the map to close.
     * @return The number of maps closed.
     */
    @NonBlocking
    @NotNull Future<Integer> destroyAll(@NotNull String mapId, @NotNull Component reason);

    /**
     * Immediately removes all maps.
     */
    void close();

    void showDebugInfo(@NotNull Audience audience);

}
