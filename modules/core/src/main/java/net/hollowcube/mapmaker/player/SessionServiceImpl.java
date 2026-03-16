package net.hollowcube.mapmaker.player;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.opentelemetry.api.OpenTelemetry;
import net.hollowcube.mapmaker.session.PlayerSession;
import net.hollowcube.mapmaker.session.SessionStateUpdateRequest;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.hollowcube.mapmaker.util.GenericServiceError;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class SessionServiceImpl extends AbstractHttpService implements SessionService {
    private static final System.Logger logger = System.getLogger(SessionServiceImpl.class.getName());

    private final String baseUrl;

    public SessionServiceImpl(OpenTelemetry otel, String url) {
        super(otel);
        this.baseUrl = String.format("%s/v3/internal", url);
    }

    @Override
    public PlayerData createSession(
        String id,
        String proxy,
        String username,
        String ip,
        PlayerSkin skin,
        String version,
        int protocolVersion
    ) {
        logger.log(System.Logger.Level.INFO, "creating new session for {0} ({1}) from {2}", id, username, ip);
        var reqBody = GSON.toJson(Map.of(
            "proxy", proxy,
            "username", username,
            "ip", ip,
            "skin", skin,
            "protocolVersion", protocolVersion,
            "version", version
        ));
        var req = HttpRequest.newBuilder()
            .method("POST", HttpRequest.BodyPublishers.ofString(reqBody))
            .uri(url("%s/session/%s", baseUrl, id))
            .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 201 -> GSON.fromJson(res.body(), PlayerData.class);
            case 403 -> {
                var error = GSON.fromJson(res.body(), JsonObject.class);
                throw new SessionCreationDeniedError(error.get("type").getAsString(), error.get("message").getAsString());
            }
            default -> throw new InternalError("Failed to create session (" + res.statusCode() + "): " + res.body());
        };
    }

    @Override
    public TransferSessionResponse transferSession(String id, SessionTransferRequest body) {
        logger.log(System.Logger.Level.INFO, "transferring session for {0}", id);
        var reqBody = GSON.toJson(body);
        var req = HttpRequest.newBuilder()
            .method("PUT", HttpRequest.BodyPublishers.ofString(reqBody))
            .uri(url("%s/session/%s", baseUrl, id))
            .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 201 -> GSON.fromJson(res.body(), TransferSessionResponse.class);
            case 401 -> throw createUnauthorizedError(res);
            default -> throw new InternalError("Failed to transfer session (" + res.statusCode() + "): " + res.body());
        };
    }

    @Override
    public void deleteSession(String id) {
        logger.log(System.Logger.Level.INFO, "deleting session for {0}", id);
        var req = HttpRequest.newBuilder()
            .method("DELETE", HttpRequest.BodyPublishers.noBody())
            .uri(url("%s/session/%s", baseUrl, id))
            .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200)
            throw new InternalError("Failed to delete session(" + res.statusCode() + "): " + res.body());
    }

    @Override
    public PlayerSession updateSessionProperties(String playerId, SessionStateUpdateRequest body) {
        logger.log(System.Logger.Level.INFO, "updating session state for {0}", playerId);
        var reqBody = GSON.toJson(body);
        var req = HttpRequest.newBuilder()
            .method("PATCH", HttpRequest.BodyPublishers.ofString(reqBody))
            .uri(url("%s/session/%s", baseUrl, playerId))
            .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200)
            throw new InternalError("Failed to update session properties (" + res.statusCode() + "): " + res.body());
        return GSON.fromJson(res.body(), PlayerSession.class);
    }

    @Override
    public List<PlayerSession> sync() {
        logger.log(System.Logger.Level.INFO, "sync sessions");
        var req = HttpRequest.newBuilder()
            .method("POST", HttpRequest.BodyPublishers.noBody())
            .uri(url("%s/server/sync", baseUrl))
            .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 200 -> GSON.fromJson(res.body(), new TypeToken<List<PlayerSession>>() {
            }.getType());
            case 204 -> List.of();
            default -> throw new InternalError("Failed to sync sessions (" + res.statusCode() + "): " + res.body());
        };
    }

    @Override
    public JoinMapResponse joinMapV2(JoinMapRequest body) {
        logger.log(System.Logger.Level.INFO, "sending join request {0}", body);
        var reqBody = GSON.toJson(body);
        var req = HttpRequest.newBuilder()
            .method("POST", HttpRequest.BodyPublishers.ofString(reqBody))
            .uri(url("%s/join_map", baseUrl))
            .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 200 -> GSON.fromJson(res.body(), JoinMapResponse.class);
            case 401 -> throw createUnauthorizedError(res);
            case 503 -> throw new NoAvailableServerException();
            default -> throw new InternalError("Failed to join map (" + res.statusCode() + "): " + res.body());
        };
    }

    @Override
    public JoinMapResponse joinHubV2(JoinHubRequest body) {
        logger.log(System.Logger.Level.INFO, "sending hub join request {0}", body);
        var reqBody = GSON.toJson(body);
        var req = HttpRequest.newBuilder()
            .method("POST", HttpRequest.BodyPublishers.ofString(reqBody))
            .uri(url("%s/join_hub", baseUrl))
            .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 200 -> GSON.fromJson(res.body(), JoinMapResponse.class);
            case 401 -> throw createUnauthorizedError(res);
            case 503 -> throw new NoAvailableServerException();
            default -> throw new InternalError("Failed to join hub (" + res.statusCode() + "): " + res.body());
        };
    }

    @Override
    public JoinMapResponse findMapServer(String mapId) {
        var req = HttpRequest.newBuilder()
            .GET()
            .uri(url("%s/maps/%s/server", baseUrl, mapId))
            .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 200 -> GSON.fromJson(res.body(), JoinMapResponse.class);
            case 401 -> throw createUnauthorizedError(res);
            case 503 -> throw new NoAvailableServerException();
            default -> throw new InternalError("Failed to findMapServer (" + res.statusCode() + "): " + res.body());
        };
    }

    private record IsolateOverride(
        String id,
        Instant lastUpdated
    ) {
    }

    @Override
    public List<String> getIsolateOverrides() {
        var req = HttpRequest.newBuilder()
            .GET()
            .uri(url("%s/server/isolate-overrides", baseUrl))
            .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 200 -> {
                var entries = GSON.fromJson(res.body(), new TypeToken<List<IsolateOverride>>() {
                });
                yield entries.stream()
                    .sorted(Comparator.comparing(a -> a.lastUpdated))
                    .map(IsolateOverride::id)
                    .toList();
            }
            case 401 -> throw createUnauthorizedError(res);
            case 503 -> throw new NoAvailableServerException();
            default -> throw new InternalError("Failed to join hub (" + res.statusCode() + "): " + res.body());
        };
    }

    private static UnauthorizedError createUnauthorizedError(HttpResponse<String> response) {
        var error = GSON.fromJson(response.body(), GenericServiceError.class);
        return new UnauthorizedError(error);
    }
}
