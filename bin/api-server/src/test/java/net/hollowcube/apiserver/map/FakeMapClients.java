package net.hollowcube.apiserver.map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import io.nats.client.Connection;
import net.hollowcube.apiserver.common.NatsPublisher;
import net.hollowcube.ipc.Wire;
import net.hollowcube.posthog.PostHogClient;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/// Nats, redis and posthog as the service sees them, recording what they were told.
final class FakeMapClients implements AutoCloseable {
    record Message(String subject, JsonObject body) {}

    final List<Message> messages = new CopyOnWriteArrayList<>();
    private final List<String> analytics = new CopyOnWriteArrayList<>();
    private final HttpServer analyticsServer;
    private boolean analyticsFinished;
    @Nullable
    RuntimeException publishFailure;

    final NatsPublisher nats;
    final PostHogClient posthog;
    final FakeRedis redis = new FakeRedis();

    FakeMapClients() {
        var connection = (Connection) Proxy.newProxyInstance(
            Connection.class.getClassLoader(),
            new Class<?>[] {Connection.class},
            (_, method, args) -> {
                if (method.getName().equals("publish") && args.length == 3) {
                    if (publishFailure != null) throw publishFailure;
                    var body = new String((byte[]) args[2], StandardCharsets.UTF_8);
                    messages.add(
                        new Message((String) args[0], Wire.gson().fromJson(body, JsonObject.class))
                    );
                }
                return null;
            }
        );
        nats = new NatsPublisher(connection, Wire.gson());
        try {
            analyticsServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        analyticsServer.createContext("/batch", exchange -> {
            try (exchange) {
                var body = JsonParser.parseString(
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)
                );
                for (var event : body.getAsJsonObject().getAsJsonArray("batch"))
                    analytics.add(event.getAsJsonObject().get("event").getAsString());
                exchange.sendResponseHeaders(200, -1);
            }
        });
        analyticsServer.start();
        posthog = PostHogClient.newBuilder("test")
            .endpoint("http://127.0.0.1:" + analyticsServer.getAddress().getPort())
            .eventBatchTimeout(Duration.ofSeconds(5))
            .build();
    }

    List<String> finishAnalytics() {
        if (!analyticsFinished) {
            posthog.shutdown(Duration.ofSeconds(5));
            analyticsFinished = true;
        }
        return List.copyOf(analytics);
    }

    @Override
    public void close() {
        finishAnalytics();
        analyticsServer.stop(0);
        redis.close();
        nats.close();
    }
}
