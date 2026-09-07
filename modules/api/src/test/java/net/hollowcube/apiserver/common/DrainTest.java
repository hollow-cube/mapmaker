package net.hollowcube.apiserver.common;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrainTest {
    private HttpServer server;
    private HttpClient client;
    private final Drain drain = new Drain();
    private final List<Integer> callerPorts = new ArrayList<>();

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var ready = server.createContext(
            "/ready",
            new Health.Ready(List.of(), null, drain::draining)
        );
        var echo = server.createContext("/echo", exchange -> {
            callerPorts.add(exchange.getRemoteAddress().getPort());
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        ready.getFilters().add(drain);
        echo.getFilters().add(drain);
        server.start();
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void stop() {
        server.stop(0);
        client.close();
    }

    @Test
    void servesNormallyUntilDraining() throws Exception {
        assertEquals(200, get("/ready").statusCode());
        var first = get("/echo");
        var second = get("/echo");
        assertTrue(first.headers().firstValue("Connection").isEmpty());
        assertTrue(second.headers().firstValue("Connection").isEmpty());
        assertEquals(callerPorts.get(0), callerPorts.get(1), "keep-alive was not reused");
    }

    @Test
    void drainingFailsReadinessAndClosesEveryConnection() throws Exception {
        get("/echo");
        drain.begin();

        var ready = get("/ready");
        assertEquals(503, ready.statusCode());
        assertEquals("close", ready.headers().firstValue("Connection").orElseThrow());

        var served = get("/echo");
        assertEquals(200, served.statusCode());
        assertEquals("close", served.headers().firstValue("Connection").orElseThrow());
        assertNotEquals(callerPorts.get(0), callerPorts.get(1), "the pooled connection survived");
    }

    private HttpResponse<Void> get(String path) throws Exception {
        var request = HttpRequest
            .newBuilder(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path))
            .build();
        return client.send(request, HttpResponse.BodyHandlers.discarding());
    }
}
