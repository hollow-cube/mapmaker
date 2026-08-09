package net.hollowcube.mapmaker.api.replays;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.opentelemetry.api.OpenTelemetry;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.api.HttpClientWrapper;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

final class ReplayClientTest {
    private final AtomicReference<Response> response = new AtomicReference<>();
    private final LinkedBlockingQueue<Request> requests = new LinkedBlockingQueue<>();

    private HttpServer server;
    private ReplayClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();

        var http = new HttpClientWrapper(
            OpenTelemetry.noop(),
            "http://127.0.0.1:" + server.getAddress().getPort()
        );
        client = new ReplayClient.Http(http);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void getsReplayPreambleAndStorageState() throws InterruptedException {
        var preamble = new byte[]{1, 2, 3, 4};
        respond(
            200,
            preamble,
            "content-type", ReplayClient.PREAMBLE_CONTENT_TYPE,
            "etag", "\"r7\"",
            "replay-state", "recording",
            "replay-representation", "segmented",
            "replay-next-segment-index", "3"
        );

        var replay = client.get("save-state");

        assertArrayEquals(preamble, replay.preamble());
        assertEquals("\"r7\"", replay.etag());
        assertEquals(ReplayState.RECORDING, replay.state());
        assertEquals(ReplayRepresentation.SEGMENTED, replay.representation());
        assertEquals(3, replay.nextSegmentIndex());

        var request = requests.take();
        assertEquals("GET", request.method());
        assertEquals("/v4/internal/replays/save-state", request.path());
        assertNull(request.query());
    }

    @Test
    void createsReplayWithPreambleFollowedBySegment() throws InterruptedException {
        respond(
            201,
            new byte[0],
            "etag", "\"r1\"",
            "replay-state", "recording",
            "replay-representation", "segmented",
            "replay-next-segment-index", "1"
        );
        var idempotencyKey = UUID.fromString("6dd447bd-40ec-4f64-b065-e9526a999593");
        var preamble = new byte[]{1, 2, 3};
        var segment = new byte[]{4, 5, 6, 7};

        var result = client.commit(
            "save-state",
            new ReplayCommitRequest(null, idempotencyKey, 0, false, preamble, segment)
        );

        assertEquals("\"r1\"", result.etag());
        assertEquals(ReplayState.RECORDING, result.state());
        assertEquals(ReplayRepresentation.SEGMENTED, result.representation());
        assertEquals(1, result.nextSegmentIndex());

        var request = requests.take();
        assertEquals("PATCH", request.method());
        assertEquals("*", request.header("if-none-match"));
        assertNull(request.header("if-match"));
        assertEquals(idempotencyKey.toString(), request.header("idempotency-key"));
        assertEquals("3", request.header("replay-preamble-length"));
        assertEquals("0", request.header("replay-segment-index"));
        assertEquals("false", request.header("replay-final"));
        assertEquals(ReplayClient.COMMIT_CONTENT_TYPE, request.header("content-type"));
        assertEquals(contentDigest(preamble, segment), request.header("content-digest"));
        assertArrayEquals(new byte[]{1, 2, 3, 4, 5, 6, 7}, request.body());
    }

    @Test
    void supportsMetadataOnlyFinalCommit() throws InterruptedException {
        respond(
            200,
            new byte[0],
            "etag", "\"r8\"",
            "replay-state", "finished",
            "replay-representation", "segmented",
            "replay-next-segment-index", "3"
        );
        var preamble = new byte[]{8, 9};

        var result = client.commit(
            "save-state",
            new ReplayCommitRequest(
                "\"r7\"",
                UUID.fromString("94f746aa-4836-412b-a399-729fafca4d25"),
                null,
                true,
                preamble,
                new byte[0]
            )
        );

        assertEquals(ReplayState.FINISHED, result.state());

        var request = requests.take();
        assertEquals("\"r7\"", request.header("if-match"));
        assertNull(request.header("if-none-match"));
        assertNull(request.header("replay-segment-index"));
        assertEquals("true", request.header("replay-final"));
        assertArrayEquals(preamble, request.body());
    }

    @Test
    void rangeReadsACommittedSegment() throws InterruptedException {
        var body = new byte[]{20, 21, 22, 23};
        respond(
            206,
            body,
            "etag", "\"segment-2\"",
            "content-range", "bytes 10-13/100"
        );

        var stream = client.getSegment("save-state", 2, ReplayRange.ofLength(10, 4));

        assertArrayEquals(body, stream.data());
        assertEquals("\"segment-2\"", stream.etag());
        assertEquals(10, stream.offset());
        assertEquals(100, stream.totalLength());

        var request = requests.take();
        assertEquals("/v4/internal/replays/save-state/stream", request.path());
        assertEquals("segment=2", request.query());
        assertEquals("bytes=10-13", request.header("range"));
    }

    @Test
    void publishesRawCompactedRecording() throws InterruptedException {
        respond(
            200,
            new byte[0],
            "etag", "\"c8\"",
            "replay-state", "finished",
            "replay-representation", "compacted"
        );
        var recording = new byte[]{1, 2, 3, 4, 5, 6};
        var idempotencyKey = UUID.fromString("2c6eb17e-02e4-4a7a-b237-c4133f969b68");

        var result = client.publishCompacted(
            "save-state",
            new ReplayCompactionRequest("\"r8\"", idempotencyKey, 4, recording)
        );

        assertEquals("\"c8\"", result.etag());
        assertEquals(ReplayRepresentation.COMPACTED, result.representation());

        var request = requests.take();
        assertEquals("PUT", request.method());
        assertEquals("\"r8\"", request.header("if-match"));
        assertEquals(idempotencyKey.toString(), request.header("idempotency-key"));
        assertEquals("4", request.header("replay-preamble-length"));
        assertEquals(ReplayClient.REPLAY_CONTENT_TYPE, request.header("content-type"));
        assertEquals(contentDigest(recording), request.header("content-digest"));
        assertArrayEquals(recording, request.body());
    }

    @Test
    void exposesOptimisticConcurrencyFailure() {
        respond(412, new byte[0]);

        var error = assertThrows(
            ApiClient.PreconditionFailedError.class,
            () -> client.commit(
                "save-state",
                new ReplayCommitRequest(
                    "\"stale\"",
                    UUID.fromString("0d7d82ab-bad4-4d31-b010-81604e80b1ba"),
                    2,
                    false,
                    new byte[]{1},
                    new byte[]{2}
                )
            )
        );

        assertEquals(412, error.statusCode());
    }

    private void respond(int status, byte[] body, String... headers) {
        var values = new LinkedHashMap<String, String>();
        for (int i = 0; i < headers.length; i += 2)
            values.put(headers[i], headers[i + 1]);
        response.set(new Response(status, body, values));
    }

    private void handle(HttpExchange exchange) throws IOException {
        var requestHeaders = new LinkedHashMap<String, List<String>>();
        exchange.getRequestHeaders().forEach((key, value) ->
            requestHeaders.put(key.toLowerCase(), List.copyOf(value))
        );
        requests.add(new Request(
            exchange.getRequestMethod(),
            exchange.getRequestURI().getPath(),
            exchange.getRequestURI().getRawQuery(),
            requestHeaders,
            exchange.getRequestBody().readAllBytes()
        ));

        var currentResponse = response.get();
        currentResponse.headers().forEach(exchange.getResponseHeaders()::add);
        exchange.sendResponseHeaders(currentResponse.status(), currentResponse.body().length);
        try (var body = exchange.getResponseBody()) {
            body.write(currentResponse.body());
        }
    }

    private static String contentDigest(byte[]... parts) {
        final MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
        for (var part : parts)
            digest.update(part);
        return "sha-256=:" + Base64.getEncoder().encodeToString(digest.digest()) + ":";
    }

    private record Request(
        String method,
        String path,
        @Nullable String query,
        Map<String, List<String>> headers,
        byte[] body
    ) {

        private @Nullable String header(String name) {
            var values = headers.get(name.toLowerCase());
            return values == null ? null : values.getFirst();
        }
    }

    private record Response(int status, byte[] body, Map<String, String> headers) {
    }
}
