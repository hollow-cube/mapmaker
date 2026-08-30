package net.hollowcube.proxy;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ProxyHttpServerTest {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ProxyHttpServerTest.class);

    @Test
    void testDisabledOnPortZero() {
        assertNull(ProxyHttpServer.start(logger, 0, () -> {}, () -> new ProxyHttpServer.Drain(0, 0),
            new CollectorRegistry()));
    }

    @Test
    void testReadyStaysUpWhileDraining() throws Exception {
        var draining = new AtomicBoolean();
        var players = new AtomicInteger(2);
        var pending = new AtomicInteger();

        var registry = new CollectorRegistry();
        Counter.build().name("proxy_test_total").help("test").register(registry).inc();

        var server = ProxyHttpServer.start(logger, freePort(), () -> draining.set(true),
            () -> new ProxyHttpServer.Drain(players.get(), pending.get()), registry);
        assertNotNull(server);
        try (server) {
            var base = "http://localhost:" + server.port();

            assertEquals(200, get(base + "/ready").statusCode());
            assertTrue(get(base + "/metrics").body().contains("proxy_test_total"));
            assertFalse(draining.get());

            var drain = get(base + "/drain");
            assertEquals(503, drain.statusCode());
            assertTrue(drain.body().contains("2 players"));
            assertTrue(draining.get());
            // The whole point: readiness does not follow the drain.
            assertEquals(200, get(base + "/ready").statusCode());

            players.set(0);
            assertEquals(200, get(base + "/drain").statusCode());
        }
    }

    /// The last player leaving is not the end of a drain: whoever this proxy transferred off is
    /// mid-reconnect elsewhere and still owes their session row a delete, so stopping the pod here
    /// would strand them.
    @Test
    void testDrainWaitsOnTransfersInFlight() throws Exception {
        var players = new AtomicInteger();
        var pending = new AtomicInteger(1);

        var server = ProxyHttpServer.start(logger, freePort(), () -> {},
            () -> new ProxyHttpServer.Drain(players.get(), pending.get()), new CollectorRegistry());
        assertNotNull(server);
        try (server) {
            var base = "http://localhost:" + server.port();

            var drain = get(base + "/drain");
            assertEquals(503, drain.statusCode());
            assertTrue(drain.body().contains("1 transfers in flight"));

            pending.set(0);
            assertEquals(200, get(base + "/drain").statusCode());
        }
    }

    private static HttpResponse<String> get(String url) throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            return client.send(HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofString());
        }
    }

    private static int freePort() throws Exception {
        try (var socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
