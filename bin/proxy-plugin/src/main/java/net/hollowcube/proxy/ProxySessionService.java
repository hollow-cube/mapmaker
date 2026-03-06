package net.hollowcube.proxy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ProxySessionService {
    private static final Gson GSON = new GsonBuilder().disableJdkUnsafe().create();
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    public static final String hostname;

    private final Logger logger;
    private final String url;

    public ProxySessionService(@NotNull Logger logger, @NotNull String url) {
        this.logger = logger;
        this.url = String.format("%s/v3/internal/session", url);
    }

    public @NotNull JsonObject createSession(@NotNull String id, @NotNull SessionCreateRequest body) throws SessionCreationDeniedError {
        logger.info("creating new session for {} ({}) from {}", id, body.username(), body.ip());
        var reqBody = GSON.toJson(body);
        var req = HttpRequest.newBuilder()
                .method("POST", HttpRequest.BodyPublishers.ofString(reqBody))
                .uri(URI.create(url + "/" + id))
                .build();
        try {
            var res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            return switch (res.statusCode()) {
                case 201 -> GSON.fromJson(res.body(), JsonObject.class);
                case 403 -> {
                    var error = GSON.fromJson(res.body(), JsonObject.class);
                    throw new SessionCreationDeniedError(error.get("type").getAsString(), error.get("message").getAsString());
                }
                default ->
                        throw new RuntimeException("Failed to create session (" + res.statusCode() + "): " + res.body());
            };
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteSession(@NotNull String id) {
        logger.info("deleting session for {}", id);
        var req = HttpRequest.newBuilder()
                .method("DELETE", HttpRequest.BodyPublishers.noBody())
                .uri(URI.create(url + "/" + id))
                .build();
        try {
            var res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 404) {
                // Its fine if not found. Means the player was removed externally.
                return;
            }
            if (res.statusCode() != 200)
                throw new RuntimeException("Failed to delete session(" + res.statusCode() + "): " + res.body());
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static final class SessionCreationDeniedError extends RuntimeException {

        private final String type;
        private final Component reason;

        public SessionCreationDeniedError(@NotNull String type, @NotNull String reason) {
            super(reason);
            this.type = type;
            this.reason = MiniMessage.miniMessage().deserialize(reason);
        }

        public @NotNull String type() {
            return type;
        }

        public @NotNull Component reason() {
            return reason;
        }
    }

    static {
        String hn;
        try {
            hn = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hn = "unknown";
        }
        hostname = hn;
    }
}
