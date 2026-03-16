package net.hollowcube.mapmaker.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.config.HttpConfig;
import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.Blocking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class HttpServerWrapper {
    private final HttpConfig config;
    private final HttpServer server;

    public HttpServerWrapper(HttpConfig config) {
        try {
            this.config = config;
            this.server = HttpServer.create(new InetSocketAddress(config.host(), config.port()), 0);

            server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        } catch (Throwable e) {
            throw new RuntimeException("Failed to create HttpServer", e);
        }
    }

    public String address() {
        return config.host();
    }

    public int port() {
        return config.port();
    }

    public void addRoute(String path, HttpHandler handler) {
        server.createContext(path, handler);
    }

    public void start() {
        this.server.start();
    }

    public void shutdown() {
        this.server.stop(5); // Give max of 5s to stop.
    }

    public record AliveHttpHandler() implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.sendResponseHeaders(200, 0);
            exchange.close();
        }
    }

    public interface HealthCheck {

        default String name() {
            return this.getClass().getSimpleName();
        }

        @Blocking boolean healthCheck();

    }

    public record ReadyHttpHandler(List<HealthCheck> checks) implements HttpHandler {
        private static final Logger logger = LoggerFactory.getLogger(ReadyHttpHandler.class);

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                final Executor executor = exchange.getHttpContext().getServer().getExecutor();
                //noinspection rawtypes
                CompletableFuture[] futures = checks.stream()
                        .map(check -> CompletableFuture.runAsync(() -> {
                            if (!check.healthCheck()) {
                                throw new RuntimeException(check.name());
                            }
                        }, executor))
                        .toArray(CompletableFuture[]::new);
                FutureUtil.getUnchecked(CompletableFuture.allOf(futures));
                exchange.sendResponseHeaders(200, 0);
            } catch (Throwable t) {
                logger.info("ready check failed: {}", t.getMessage());
                exchange.sendResponseHeaders(503, 0);
            } finally {
                exchange.close();
            }
        }
    }

    public record PlayerStatusHandler() implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            var players = new JsonArray();
            for (var player : MinecraftServer.getConnectionManager().getOnlinePlayers())
                players.add(player.getUuid().toString());
            for (var player : MinecraftServer.getConnectionManager().getConfigPlayers())
                players.add(player.getUuid().toString());

            var response = new JsonObject();
            response.add("players", players);
            var responseBytes = response.toString().getBytes(StandardCharsets.UTF_8);

            exchange.sendResponseHeaders(200, responseBytes.length);
            exchange.getResponseBody().write(responseBytes);
            exchange.close();
        }
    }
}
