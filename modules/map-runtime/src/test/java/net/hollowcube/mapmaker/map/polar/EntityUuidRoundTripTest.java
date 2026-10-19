package net.hollowcube.mapmaker.map.polar;

import net.hollowcube.mapmaker.map.ReadableMapData;
import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.hollowcube.mapmaker.test.AbstractMapWorldTest;
import net.hollowcube.mapmaker.test.TestMapWorld;
import net.minestom.server.coordinate.Pos;
import net.minestom.testing.Env;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.channels.Channels;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class EntityUuidRoundTripTest extends AbstractMapWorldTest<TestMapWorld> {

    @Override
    protected @NotNull TestMapWorld createWorld(@NotNull Env env) {
        var world = new TestMapWorld();
        world.instance().loadChunk(0, 0).join();
        return world;
    }

    @Test
    void markerKeepsItsUuidAcrossSaveAndLoad() {
        // Marker checkpoint and status ids are the marker's UUID and are kept in save states, so they must survive
        // the world being saved and loaded again.
        var marker = new MarkerEntity();
        marker.setInstance(world.instance(), new Pos(4, 40, 4)).join();
        env.tick();

        var bytes = world.instance().save(new ReadWriteWorldAccess(world));
        var loaded = new TestMapWorld();
        loaded.instance().loadStream(new ReadableMapData(Channels.newChannel(new ByteArrayInputStream(bytes)), bytes.length),
            new ReadWorldAccess(loaded));
        env.tick();

        assertNotNull(loaded.instance().getEntityByUuid(marker.getUuid()));
        loaded.instance().unload();
    }
}
