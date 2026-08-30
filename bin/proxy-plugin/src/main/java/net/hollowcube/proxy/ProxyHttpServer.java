package net.hollowcube.proxy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.function.IntSupplier;

/// The proxy's http side, which is what the deployment talks to rather than a player:
///
/// - `/ready` is 200 from the moment velocity is listening, and stays 200 for the whole life of
///   the process, drain included. Cilium only keeps established connections flowing to a pod
///   that is terminating *and* still serving; a readiness that fails during a drain would take
///   the backend out of the load balancer map and reset every player still on it.
/// - `/drain` tells the plugin to stop taking logins and answers 200 once nobody is connected,
///   503 while somebody still is. The pod's preStop hook polls it until 200, and only then does
///   kubernetes send the SIGTERM velocity answers by disconnecting everyone.
public final class ProxyHttpServer implements AutoCloseable {
    private final HttpServer server;

    private ProxyHttpServer(HttpServer server) {
        this.server = server;
    }

    /// Serves on `port`, or returns null for 0 or a port that cannot be bound. Never throws: the
    /// proxy must come up whether or not it has an http side, which is how a dev run without one
    /// behaves; in the cluster a missing `/ready` keeps the pod out of the rollout, which is right.
    public static @Nullable ProxyHttpServer start(@NotNull Logger logger, int port,
                                                 @NotNull Runnable startDrain, @NotNull IntSupplier playerCount) {
        if (port == 0) {
            logger.info("proxy http disabled (PROXY_HTTP_PORT=0)");
            return null;
        }
        try {
            var server = HttpServer.create(new InetSocketAddress(port), 0);
            server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
            server.createContext("/ready", exchange -> reply(exchange, 200, "ok"));
            server.createContext("/drain", exchange -> {
                startDrain.run();
                int players = playerCount.getAsInt();
                if (players == 0) reply(exchange, 200, "drained");
                else reply(exchange, 503, "draining, " + players + " players connected");
            });
            server.start();
            logger.info("proxy http on :{} (/ready, /drain)", server.getAddress().getPort());
            return new ProxyHttpServer(server);
        } catch (IOException e) {
            logger.error("proxy http failed to bind :{}", port, e);
            return null;
        }
    }

    public int port() {
        return server.getAddress().getPort();
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private static void reply(HttpExchange exchange, int status, String body) throws IOException {
        var bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
