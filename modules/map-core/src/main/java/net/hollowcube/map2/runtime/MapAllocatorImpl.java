package net.hollowcube.map2.runtime;

import net.hollowcube.map2.AbstractMapWorld;
import net.hollowcube.map2.MapServer;
import net.hollowcube.mapmaker.map.MapData;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

final class MapAllocatorImpl implements MapAllocator {
    private static final Logger logger = LoggerFactory.getLogger(MapAllocatorImpl.class);

    private final MapServer server;

    MapAllocatorImpl(@NotNull MapServer server) {
        this.server = server;
    }

    @Override
    public <T extends AbstractMapWorld> @NotNull T allocateDirect(@NotNull MapData map, @NotNull Class<T> worldType) {
        var world = server.createInstance(worldType, Map.of(MapData.class, map));
        world.load();
        return world;
    }

    @Override
    public void close() {

    }
}
