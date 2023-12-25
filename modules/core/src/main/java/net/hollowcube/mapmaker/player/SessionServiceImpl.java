package net.hollowcube.mapmaker.player;

import net.hollowcube.mapmaker.util.AbstractHttpService;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SessionServiceImpl extends AbstractHttpService implements SessionService {
    private static final System.Logger logger = System.getLogger(SessionServiceImpl.class.getName());

    private final String url;
    private final String urlV2;
    private final String urlShortV2;

    public SessionServiceImpl(@NotNull String url) {
        this.url = String.format("%s/v1/internal/session", url);
        this.urlV2 = String.format("%s/v2/internal/session", url);
        this.urlShortV2 = String.format("%s/v2/internal", url);
    }

    @Override
    public @NotNull PlayerDataV2 createSession(@NotNull String id, @NotNull String username, @NotNull String ip) {
        logger.log(System.Logger.Level.INFO, "creating new session for {0} ({1})", id, username, ip);
        var reqBody = GSON.toJson(new SessionCreateRequest(hostname, username, ip));
        var req = HttpRequest.newBuilder()
                .method("POST", HttpRequest.BodyPublishers.ofString(reqBody))
                .uri(URI.create(url + "/" + id))
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 201 -> GSON.fromJson(res.body(), PlayerDataV2.class);
            case 401 -> throw new UnauthorizedError();
            default -> throw new InternalError("Failed to create session (" + res.statusCode() + "): " + res.body());
        };
    }

    @Override
    public void deleteSession(@NotNull String id) {
        logger.log(System.Logger.Level.INFO, "deleted session for {0}", id);
        var req = HttpRequest.newBuilder()
                .method("DELETE", HttpRequest.BodyPublishers.noBody())
                .uri(URI.create(url + "/" + id))
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200)
            throw new InternalError("Failed to delete session: " + res.body());
    }

    @Override
    public @NotNull PlayerDataV2 createSessionV2(@NotNull String id, @NotNull String username, @NotNull String ip) {
        logger.log(System.Logger.Level.INFO, "creating new session for {0} ({1}) from {2}", id, username, ip);
        var reqBody = GSON.toJson(new SessionCreateRequest(hostname, username, ip));
        var req = HttpRequest.newBuilder()
                .method("POST", HttpRequest.BodyPublishers.ofString(reqBody))
                .uri(URI.create(urlV2 + "/" + id))
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 201 -> GSON.fromJson(res.body(), PlayerDataV2.class);
            case 401 -> throw new UnauthorizedError();
            default -> throw new InternalError("Failed to create session (" + res.statusCode() + "): " + res.body());
        };
    }

    @Override
    public @NotNull PlayerDataV2 transferSessionV2(@NotNull String id, @NotNull SessionTransferRequest body) {
        logger.log(System.Logger.Level.INFO, "transferring session for {0}", id);
        var reqBody = GSON.toJson(body);
        var req = HttpRequest.newBuilder()
                .method("PUT", HttpRequest.BodyPublishers.ofString(reqBody))
                .uri(URI.create(urlV2 + "/" + id))
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 201 -> GSON.fromJson(res.body(), PlayerDataV2.class);
            case 401 -> throw new UnauthorizedError();
            default -> throw new InternalError("Failed to create session (" + res.statusCode() + "): " + res.body());
        };
    }

    @Override
    public void deleteSessionV2(@NotNull String id) {
        logger.log(System.Logger.Level.INFO, "deleting session for {0}", id);
        var req = HttpRequest.newBuilder()
                .method("DELETE", HttpRequest.BodyPublishers.noBody())
                .uri(URI.create(urlV2 + "/" + id))
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200)
            throw new InternalError("Failed to delete session(" + res.statusCode() + "): " + res.body());
    }

    @Override
    public @NotNull JoinMapResponse joinMapV2(@NotNull JoinMapRequest body) {
        logger.log(System.Logger.Level.INFO, "sending join request {0}", body);
        var reqBody = GSON.toJson(body);
        var req = HttpRequest.newBuilder()
                .method("POST", HttpRequest.BodyPublishers.ofString(reqBody))
                .uri(URI.create(urlShortV2 + "/join_map"))
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 200 -> GSON.fromJson(res.body(), JoinMapResponse.class);
            case 401 -> throw new UnauthorizedError();
            default -> throw new InternalError("Failed to join map (" + res.statusCode() + "): " + res.body());
        };
    }

    @Override
    public @NotNull JoinMapResponse joinHubV2(@NotNull JoinHubRequest body) {
        logger.log(System.Logger.Level.INFO, "sending hub join request {0}", body);
        var reqBody = GSON.toJson(body);
        var req = HttpRequest.newBuilder()
                .method("POST", HttpRequest.BodyPublishers.ofString(reqBody))
                .uri(URI.create(urlShortV2 + "/join_hub"))
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 200 -> GSON.fromJson(res.body(), JoinMapResponse.class);
            case 401 -> throw new UnauthorizedError();
            default -> throw new InternalError("Failed to join hub (" + res.statusCode() + "): " + res.body());
        };
    }
}
