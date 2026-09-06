package net.hollowcube.mapmaker.api.maps;

import com.sun.net.httpserver.HttpServer;
import io.opentelemetry.api.OpenTelemetry;
import net.hollowcube.ipc.map.*;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.api.HttpClientWrapper;
import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.map.MapTags;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class MapClientTest {
    private final MapData map = MapData.draft(UUID.randomUUID(), UUID.randomUUID());
    private final AtomicReference<Object[]> invocation = new AtomicReference<>();
    private HttpServer server;
    private MapClient client;

    @BeforeEach
    void start() throws Exception {
        var service = (MapService) Proxy.newProxyInstance(MapService.class.getClassLoader(),
            new Class<?>[]{MapService.class}, (_, method, args) -> {
                invocation.set(args);
                return switch (method.getName()) {
                    case "create" -> new CreateMapResult.Success(map);
                    case "get" -> "missing".equals(args[0]) ? null : map;
                    case "getPlayerSlots" -> List.of(new MapSlot(map, Instant.EPOCH, true,
                        List.of(new MapBuilder(UUID.randomUUID(), Instant.EPOCH, true))));
                    case "update" -> null;
                    default -> throw new UnsupportedOperationException(method.getName());
                };
            });
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(MapServer.PATH, new MapServer(service));
        server.start();
        var url = "http://127.0.0.1:" + server.getAddress().getPort();
        client = new MapClient.Http(new HttpClientWrapper(OpenTelemetry.noop(), url),
            new net.hollowcube.ipc.map.MapClient(HttpClient.newHttpClient(), url));
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void creationPassesTheProtocolVersionAndReturnsTheTypedResult() {
        var result = assertInstanceOf(CreateMapResult.Success.class,
            client.create(map.owner().toString(), MapSize.LARGE));
        assertEquals(map, result.map());
        assertArrayEquals(new Object[]{map.owner(), MapSize.LARGE, MinecraftServer.PROTOCOL_VERSION},
            invocation.get());
    }

    @Test
    void readsReturnSharedSlotsAndPreserveTheMissingMapException() {
        var slots = client.getPlayerSlots(map.owner().toString()).results();
        assertTrue(slots.getFirst().owner());
        assertEquals(map, slots.getFirst().map());
        assertTrue(slots.getFirst().builders().getFirst().pending());
        assertThrows(ApiClient.NotFoundError.class, () -> client.get("missing"));
    }

    @Test
    void builderSavesSendThePatchOverIpc() {
        var builder = new MapPatch.Builder(client.get(map.id().toString()));
        builder.setSize(MapSize.LARGE);
        builder.setSpawnPoint(MapSettings.position(new Pos(1, 2, 3, 90, 45)));
        builder.setLeaderboard(new MapLeaderboard(false, MapLeaderboard.Format.NUMBER, "q.score"));
        MapSettings.addTag(builder, MapTags.Tag.TERRAIN);
        MapSettings.set(builder, MapSettings.NO_JUMP, true);
        var expected = builder.build();
        builder.save(patch -> {
            client.update(builder.map().id().toString(), patch);
            return builder.map();
        });
        assertEquals(map.id(), invocation.get()[0]);
        assertEquals(expected, invocation.get()[1]);
        assertTrue(builder.build().isEmpty());
    }
}
