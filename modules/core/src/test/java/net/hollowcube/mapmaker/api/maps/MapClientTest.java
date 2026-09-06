package net.hollowcube.mapmaker.api.maps;

import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import io.opentelemetry.api.OpenTelemetry;
import net.hollowcube.ipc.map.MapLeaderboard;
import net.hollowcube.ipc.map.MapPatch;
import net.hollowcube.ipc.map.MapSize;
import net.hollowcube.mapmaker.api.HttpClientWrapper;
import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.map.MapTags;
import net.minestom.server.coordinate.Pos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class MapClientTest {
    private static final String MAP = """
        {"id":"00000000-0000-0000-0000-000000000001",
        "owner":"00000000-0000-0000-0000-000000000002",
        "publishedId":123,"settings":{"name":"A map","size":"normal","variant":"parkour"}}
        """;
    private final LinkedBlockingQueue<Request> requests = new LinkedBlockingQueue<>();
    private volatile String response = MAP;
    private HttpServer server;
    private MapClient client;

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.add(new Request(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
                new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
            var bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (var output = exchange.getResponseBody()) { output.write(bytes); }
        });
        server.start();
        client = new MapClient.Http(new HttpClientWrapper(OpenTelemetry.noop(),
            "http://127.0.0.1:" + server.getAddress().getPort()));
    }

    @AfterEach
    void stop() { server.stop(0); }

    @Test
    void creationKeepsTheGoRequestAndReturnsSharedMapData() throws Exception {
        var map = client.create("owner", MapSize.LARGE);
        assertEquals("000-000-123", map.publishedId());
        assertEquals("A map", map.name());
        var request = requests.poll(5, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("POST", request.method());
        assertEquals("/v4/internal/maps", request.path());
        assertEquals("large", JsonParser.parseString(request.body()).getAsJsonObject().get("size").getAsString());
    }

    @Test
    void slotsDecodeLegacyRolesAndNullableBuilderLists() {
        response = "{\"results\":[{\"map\":" + MAP + ",\"createdAt\":\"2026-09-06T00:00:00Z\",\"role\":\"owner\",\"builders\":null},"
            + "{\"map\":" + MAP + ",\"createdAt\":\"2026-09-06T00:00:00Z\",\"role\":\"builder\",\"builders\":[{\"id\":\"00000000-0000-0000-0000-000000000003\",\"createdAt\":\"2026-09-06T00:00:00Z\",\"pending\":true}]}]}";
        var slots = client.getPlayerSlots("player").results();
        assertTrue(slots.getFirst().owner());
        assertTrue(slots.getFirst().builders().isEmpty());
        assertFalse(slots.getLast().owner());
        assertEquals("00000000-0000-0000-0000-000000000003", slots.getLast().builders().getFirst().id().toString());
        assertTrue(slots.getLast().builders().getFirst().pending());
        assertEquals("000-000-123", slots.getFirst().map().publishedId());
    }

    @Test
    void editsStillUseTheGoPatchShape() throws Exception {
        var editor = new MapPatch.Builder(client.get("map"));
        requests.clear();
        editor.setSize(MapSize.LARGE);
        editor.setSpawnPoint(MapSettings.position(new Pos(1, 2, 3, 90, 45)));
        editor.setLeaderboard(new MapLeaderboard(false, MapLeaderboard.Format.NUMBER, "q.score"));
        MapSettings.addTag(editor, MapTags.Tag.TERRAIN);
        MapSettings.set(editor, MapSettings.NO_JUMP, true);
        editor.save(request -> {
            client.update(editor.map().id().toString(), request);
            return editor.map();
        });
        var request = requests.poll(5, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("PATCH", request.method());
        assertEquals("/v4/internal/maps/00000000-0000-0000-0000-000000000001", request.path());
        var body = JsonParser.parseString(request.body()).getAsJsonObject();
        assertEquals("large", body.get("size").getAsString());
        assertEquals(90, body.getAsJsonObject("spawnPoint").get("yaw").getAsInt());
        assertEquals("number", body.getAsJsonObject("leaderboard").get("format").getAsString());
        assertEquals("terrain", body.getAsJsonArray("tags").get(0).getAsString());
        assertTrue(body.getAsJsonObject("extra").get("no_jump").getAsBoolean());
    }

    private record Request(String method, String path, String body) {}
}
