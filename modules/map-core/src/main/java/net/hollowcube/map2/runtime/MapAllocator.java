package net.hollowcube.map2.runtime;

import net.hollowcube.map2.AbstractMapWorld;
import net.hollowcube.map2.MapServer;
import net.hollowcube.mapmaker.map.MapData;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

/**
 * Responsible for creating map worlds, maybe who knows.
 */
public interface MapAllocator {

    static @NotNull MapAllocator create(@NotNull MapServer server) {
        return new MapAllocatorImpl(server);
    }

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
    @Blocking
    <T extends AbstractMapWorld> @NotNull T allocateDirect(@NotNull MapData map, @NotNull Class<T> worldType);

    /**
     * Immediately removes all maps.
     */
    void close();

}
