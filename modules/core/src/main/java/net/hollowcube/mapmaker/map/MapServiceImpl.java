package net.hollowcube.mapmaker.map;

import net.hollowcube.mapmaker.util.AbstractHttpService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

public class MapServiceImpl extends AbstractHttpService implements MapService {
    private static final System.Logger logger = System.getLogger(MapServiceImpl.class.getName());

    private static final String AUTHORIZER_HEADER = "x-hc-user-id";
    private static final String WORLD_SESSION_HEADER = "x-hc-map-session";

    private static final String POLAR_CONTENT_TYPE = "application/vnd.hollowcube.polar";

    private final String url;

    public MapServiceImpl(String url) {
        this.url = String.format("%s/v1/internal/maps", url);
    }

    @Override
    public @NotNull MapData createMap(@NotNull String authorizer, @NotNull String owner) {
        logger.log(System.Logger.Level.INFO, "creating new map for " + owner);
        var reqBody = GSON.toJson(Map.of("owner", owner));
        var req = HttpRequest.newBuilder()
                .method("POST", HttpRequest.BodyPublishers.ofString(reqBody))
                .uri(URI.create(url))
                .header(AUTHORIZER_HEADER, authorizer)
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200)
            throw new InternalError("Failed to create map: " + res.body());

        return GSON.fromJson(res.body(), MapData.class);
    }

    @Override
    public @NotNull MapSearchResponse searchMaps(@NotNull String authorizer, int page, boolean building, boolean parkour, @NotNull String query) {
        logger.log(System.Logger.Level.INFO, "searching maps for " + query);
        var req = HttpRequest.newBuilder()
                .uri(URI.create(url + "/search?page=" + page + "&query=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&building=" + building + "&parkour=" + parkour))
                .header(AUTHORIZER_HEADER, authorizer)
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 200 -> GSON.fromJson(res.body(), MapSearchResponse.class);
            default -> throw new InternalError("Failed to search maps: " + res.body());
        };
    }

    @Override
    public @NotNull MapData getMap(@NotNull String authorizer, @NotNull String id) {
        logger.log(System.Logger.Level.INFO, "getting map " + id);
        var req = HttpRequest.newBuilder()
                .uri(URI.create(url + "/" + id))
                .header(AUTHORIZER_HEADER, authorizer)
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 200 -> GSON.fromJson(res.body(), MapData.class);
            case 404 -> throw new NotFoundError(id);
            default -> throw new InternalError("Failed to get map: " + res.body());
        };
    }

    @Override
    public void updateMap(@NotNull String authorizer, @NotNull String id, @NotNull MapUpdateRequest update) {
        logger.log(System.Logger.Level.INFO, "updating map " + id);
        var reqBody = GSON.toJson(update);
        var req = HttpRequest.newBuilder()
                .method("PATCH", HttpRequest.BodyPublishers.ofString(reqBody))
                .uri(URI.create(url + "/" + id))
                .header(AUTHORIZER_HEADER, authorizer)
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        switch (res.statusCode()) {
            case 204 -> {/* update ok */}
            case 404 -> throw new NotFoundError(id);
            default -> throw new InternalError("Failed to update map: " + res.body());
        };
    }

    @Override
    public void deleteMap(@NotNull String authorizer, @NotNull String id) {
        logger.log(System.Logger.Level.INFO, "deleting map " + id);
        var req = HttpRequest.newBuilder()
                .method("DELETE", HttpRequest.BodyPublishers.noBody())
                .uri(URI.create(url + "/" + id))
                .header(AUTHORIZER_HEADER, authorizer)
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        switch (res.statusCode()) {
            case 204 -> {/* delete ok */}
            case 404 -> throw new NotFoundError(id);
            default -> throw new InternalError("Failed to delete map: " + res.body());
        }
    }

    @Override
    public void beginVerification(@NotNull String authorizer, @NotNull String mapId) {
        logger.log(System.Logger.Level.INFO, "beginning verification for map " + mapId);
        var req = HttpRequest.newBuilder()
                .method("POST", HttpRequest.BodyPublishers.noBody())
                .uri(URI.create(url + "/" + mapId + "/verify"))
                .header(AUTHORIZER_HEADER, authorizer)
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        switch (res.statusCode()) {
            case 200 -> {/* verification ok */}
            case 404 -> throw new NotFoundError(mapId);
            default -> throw new InternalError("Failed to begin verification: " + res.body());
        }
    }

    @Override
    public void deleteVerification(@NotNull String authorizer, @NotNull String mapId) {
        logger.log(System.Logger.Level.INFO, "deleting verification for map " + mapId);
        var req = HttpRequest.newBuilder()
                .method("DELETE", HttpRequest.BodyPublishers.noBody())
                .uri(URI.create(url + "/" + mapId + "/verify"))
                .header(AUTHORIZER_HEADER, authorizer)
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        switch (res.statusCode()) {
            case 200 -> {/* deleted */}
            case 404 -> throw new NotFoundError(mapId);
            default -> throw new InternalError("Failed to delete verification (" + res.statusCode() + "): " + res.body());
        }
    }

    @Override
    public @NotNull MapData publishMap(@NotNull String authorizer, @NotNull String id) {
        logger.log(System.Logger.Level.INFO, "publishing map " + id);
        var req = HttpRequest.newBuilder()
                .method("POST", HttpRequest.BodyPublishers.noBody())
                .uri(URI.create(url + "/" + id + "/publish"))
                .header(AUTHORIZER_HEADER, authorizer)
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 200 -> GSON.fromJson(res.body(), MapData.class);
            case 404 -> throw new NotFoundError(id);
            default -> throw new InternalError("Failed to publish map: " + res.body());
        };
    }

    @Override
    public byte @Nullable [] getMapWorld(@NotNull String id, boolean write) {
        var req = HttpRequest.newBuilder()
                .uri(URI.create(url + "/" + id + "/world?scope=" + (write ? "write" : "read")))
                .header(AUTHORIZER_HEADER, UUID.randomUUID().toString()) //todo
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofByteArray());
        return switch (res.statusCode()) {
            case 200 -> res.body();
            case 204 -> null;
            case 404 -> throw new NotFoundError(id);
            default -> throw new InternalError("Failed to get map world: " + new String(res.body()));
        };
    }

    @Override
    public void updateMapWorld(@NotNull String id, byte @NotNull [] worldData) {
        var req = HttpRequest.newBuilder()
                .method("PUT", HttpRequest.BodyPublishers.ofByteArray(worldData))
                .uri(URI.create(url + "/" + id + "/world"))
                .header(AUTHORIZER_HEADER, UUID.randomUUID().toString()) //todo
                .header(WORLD_SESSION_HEADER, "todo")
                .header("content-type", POLAR_CONTENT_TYPE)
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        switch (res.statusCode()) {
            case 204 -> {/* update ok */}
            case 404 -> throw new NotFoundError(id);
            default -> throw new InternalError("Failed to update map world: " + res.body());
        }
    }

    @Override
    public @NotNull LeaderboardData getPlaytimeLeaderboard(@NotNull String mapId, @Nullable String playerId) {
        var uri = url + "/" + mapId + "/leaderboard/playtime";
        if (playerId != null) uri += "?playerId=" + playerId;
        var req = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header(AUTHORIZER_HEADER, UUID.randomUUID().toString()) //todo
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 200 -> GSON.fromJson(res.body(), LeaderboardData.class);
            default -> throw new InternalError("Failed to get playtime leaderboard: " + res.body());
        };
    }

    @Override
    public @NotNull SaveState createSaveState(@NotNull String mapId, @NotNull String playerId) {
        var req = HttpRequest.newBuilder()
                .method("POST", HttpRequest.BodyPublishers.noBody())
                .uri(URI.create(url + "/" + mapId + "/savestates/" + playerId))
                .header(AUTHORIZER_HEADER, UUID.randomUUID().toString()) //todo
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 201 -> GSON.fromJson(res.body(), SaveState.class);
            default -> throw new InternalError("Failed to create savestate: " + res.body());
        };
    }

    @Override
    public @NotNull SaveState getSaveState(@NotNull String mapId, @NotNull String playerId, @NotNull String id) {
        var req = HttpRequest.newBuilder()
                .uri(URI.create(url + "/" + mapId + "/savestates/" + playerId + "/" + id))
                .header(AUTHORIZER_HEADER, UUID.randomUUID().toString()) //todo
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 200 -> GSON.fromJson(res.body(), SaveState.class);
            case 404 -> throw new NotFoundError(id);
            default -> throw new InternalError("Failed to get savestate: " + res.body());
        };
    }

    @Override
    public @NotNull SaveState getLatestSaveState(@NotNull String mapId, @NotNull String playerId) {
        var req = HttpRequest.newBuilder()
                .uri(URI.create(url + "/" + mapId + "/savestates/" + playerId + "/latest"))
                .header(AUTHORIZER_HEADER, UUID.randomUUID().toString()) //todo
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 200 -> GSON.fromJson(res.body(), SaveState.class);
            case 404 -> throw new NotFoundError("latest");
            default -> throw new InternalError("Failed to get latest savestate: " + res.body());
        };
    }

    @Override
    public void updateSaveState(@NotNull String mapId, @NotNull String playerId, @NotNull String id, @NotNull SaveStateUpdateRequest update) {
        var reqBody = GSON.toJson(update);
        var req = HttpRequest.newBuilder()
                .method("PATCH", HttpRequest.BodyPublishers.ofString(reqBody))
                .uri(URI.create(url + "/" + mapId + "/savestates/" + playerId + "/" + id))
                .header(AUTHORIZER_HEADER, UUID.randomUUID().toString()) //todo
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() == 204) return; // Ok
        throw new InternalError("Failed to update savestate: " + res.body());
    }

    @Override
    public void deleteSaveState(@NotNull String mapId, @NotNull String playerId, @NotNull String id) {
        var req = HttpRequest.newBuilder()
                .method("DELETE", HttpRequest.BodyPublishers.noBody())
                .uri(URI.create(url + "/" + mapId + "/savestates/" + playerId + "/" + id))
                .header(AUTHORIZER_HEADER, UUID.randomUUID().toString()) //todo
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() == 204) return; // Ok
        throw new InternalError("Failed to delete savestate: " + res.body());
    }
}
