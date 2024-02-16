package net.hollowcube.map2.runtime;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import net.hollowcube.map2.AbstractMapWorld;
import net.hollowcube.map2.MapServer;
import net.hollowcube.mapmaker.map.MapData;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class MapAllocatorImpl implements MapAllocator {
    private static final Logger logger = LoggerFactory.getLogger(MapAllocatorImpl.class);

    private final MapServer server;

    MapAllocatorImpl(@NotNull MapServer server) {
        this.server = server;
    }

    @Override
    public <T extends AbstractMapWorld> @NotNull T allocateDirect(@NotNull MapData map, @NotNull Class<T> worldType) {
        var injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(MapData.class).toInstance(map);
                bind(MapServer.class).toInstance(server);
            }
        });

        var world = injector.getInstance(worldType);
//        var world = server.createInstance(worldType);
        world.load();
        return world;
    }

    @Override
    public void close() {

    }
}
