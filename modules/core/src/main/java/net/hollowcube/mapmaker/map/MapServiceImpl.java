package net.hollowcube.mapmaker.map;

import com.google.gson.reflect.TypeToken;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class MapServiceImpl extends AbstractHttpService implements MapService {
    private static final System.Logger logger = System.getLogger(MapServiceImpl.class.getName());

    private static final String AUTHORIZER_HEADER = "x-hc-user-id";
    private static final String WORLD_SESSION_HEADER = "x-hc-map-session";

    private static final String POLAR_CONTENT_TYPE = "application/vnd.hollowcube.polar";

    private final String url;
    private final String legacyUrl;
    private final String perfdumpUrl;

    public MapServiceImpl(String url) {
        this.url = String.format("%s/v1/internal/maps", url);
        this.legacyUrl = String.format("%s/v1/internal/legacy/maps", url);
        this.perfdumpUrl = String.format("%s/v1/internal/perfdump", url);
    }

    @Override
    public @NotNull MapData createMap(@NotNull MapPlayerData player, int slot) {
        logger.log(System.Logger.Level.INFO, "creating new map for " + player.id());
        var reqBody = GSON.toJson(Map.of("owner", player.id(), "slot", slot));
        var req = HttpRequest.newBuilder()
                .method("POST", HttpRequest.BodyPublishers.ofString(reqBody))
                .uri(URI.create(url))
                .header(AUTHORIZER_HEADER, player.id())
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 201)
            throw new InternalError("Failed to create map: " + res.body());

        return GSON.fromJson(res.body(), MapData.class);
    }

    @Override
    public @NotNull MapSearchResponse searchMaps(@NotNull String authorizer, int page, int pageSize, boolean building, boolean parkour, @NotNull String query) {
        Check.argCondition(pageSize > 50, "pageSize must be less than or equal to 50");
        logger.log(System.Logger.Level.INFO, "searching maps for " + query);
        var req = HttpRequest.newBuilder()
                .uri(URI.create(url + "/search?page=" + page + "&pageSize=" + pageSize + "&query=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&building=" + building + "&parkour=" + parkour))
                .header(AUTHORIZER_HEADER, authorizer)
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 200 -> GSON.fromJson(res.body(), MapSearchResponse.class);
            default -> throw new InternalError("Failed to search maps: " + res.body());
        };
    }

    @Override
    public @NotNull MapSearchResponse searchMaps(@NotNull MapSearchRequest params) {
        var endpoint = new StringBuilder(url + "/search?");
        if (params.page() != -1) {
            endpoint.append("page=").append(params.page()).append("&");
            endpoint.append("pageSize=").append(params.pageSize()).append("&");
        }
        if (params.owner() != null) {
            endpoint.append("owner=").append(params.owner()).append("&");
        }

        var req = HttpRequest.newBuilder()
                .uri(URI.create(endpoint.toString()))
                .header(AUTHORIZER_HEADER, params.authorizer())
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
    public @NotNull MapData getMapByPublishedId(@NotNull String authorizer, long publishedId) {
        logger.log(System.Logger.Level.INFO, "getting map by published id " + publishedId);
        var req = HttpRequest.newBuilder()
                .uri(URI.create(url + "/" + publishedId))
                .header(AUTHORIZER_HEADER, authorizer)
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 200 -> GSON.fromJson(res.body(), MapData.class);
            case 404 -> throw new NotFoundError(MapData.formatPublishedId(publishedId));
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
            default -> throw new InternalError("Failed to update map" + res.statusCode() + ": " + res.body());
        }
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
            case 200 -> {/* delete ok */}
            case 404 -> throw new NotFoundError(id);
            default -> throw new InternalError("Failed to delete map" + res.statusCode() + ": " + res.body());
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
            default ->
                    throw new InternalError("Failed to delete verification (" + res.statusCode() + "): " + res.body());
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
    public void deletePlaytimeLeaderboard(@NotNull String authorizer, @NotNull String mapId, @Nullable String playerId) {
        var uri = url + "/" + mapId + "/leaderboard/playtime";
        if (playerId != null) uri += "?playerId=" + playerId;
        var req = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .method("DELETE", HttpRequest.BodyPublishers.noBody())
                .header(AUTHORIZER_HEADER, authorizer)
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.discarding());
        switch (res.statusCode()) {
            case 200 -> {
            }
            default -> throw new InternalError("Failed to delete playtime leaderboard: " + res.body());
        }
    }

    @Override
    public void restorePlaytimeLeaderboard(@NotNull String authorizer, @NotNull String mapId) {
        var uri = url + "/" + mapId + "/leaderboard/playtime/restore";
        var req = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .method("POST", HttpRequest.BodyPublishers.noBody())
                .header(AUTHORIZER_HEADER, authorizer)
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.discarding());
        switch (res.statusCode()) {
            case 200 -> {
            }
            default -> throw new InternalError("Failed to restore playtime leaderboard: " + res.body());
        }
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
    public @NotNull SaveState getLatestSaveState(@NotNull String mapId, @NotNull String playerId, @Nullable SaveStateType type) {
        var req = HttpRequest.newBuilder()
                .uri(URI.create(url + "/" + mapId + "/savestates/" + playerId + "/latest?typeFilter=" + (type == null ? "" : type.name().toLowerCase(Locale.ROOT))))
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
    public @Nullable SaveState getBestSaveState(@NotNull String mapId, @NotNull String playerId) {
        var req = HttpRequest.newBuilder()
                .uri(URI.create(url + "/" + mapId + "/savestates/" + playerId + "/best"))
                .header(AUTHORIZER_HEADER, UUID.randomUUID().toString()) //todo
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 200 -> GSON.fromJson(res.body(), SaveState.class);
            case 404 -> null;
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
        if (res.statusCode() == 200) return; // Ok
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

    @Override
    public @Nullable InputStream getSaveStateReplay(@NotNull String mapId, @NotNull String playerId, @NotNull String saveStateId) {
        var req = HttpRequest.newBuilder()
                .uri(URI.create(url + "/" + mapId + "/savestates/" + playerId + "/" + saveStateId + "/replay"))
                .header(AUTHORIZER_HEADER, playerId)
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofInputStream());
        if (res.statusCode() == 200) return res.body(); // Ok
        if (res.statusCode() == 404) return null; // Not found
        throw new InternalError("Failed to get savestate replay: " + res.statusCode());
    }

    @Override
    public void updateSaveStateReplay(@NotNull String mapId, @NotNull String playerId, @NotNull String saveStateId, @NotNull InputStream dataStream) {
        var req = HttpRequest.newBuilder()
                .method("PUT", HttpRequest.BodyPublishers.ofInputStream(() -> dataStream))
                .uri(URI.create(url + "/" + mapId + "/savestates/" + playerId + "/" + saveStateId + "/replay"))
                .header(AUTHORIZER_HEADER, playerId)
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() == 200) return; // Ok
        throw new InternalError("Failed to update savestate replay: " + res.body());
    }

    @Override
    public @NotNull MapRating getMapRating(@NotNull String mapId, @NotNull String playerId) {
        var req = HttpRequest.newBuilder()
                .uri(URI.create(url + "/" + mapId + "/ratings/" + playerId))
                .header(AUTHORIZER_HEADER, playerId)
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 200 -> GSON.fromJson(res.body(), MapRating.class);
            case 404 -> new MapRating();
            default -> throw new InternalError("Failed to get map rating (" + res.statusCode() + "): " + res.body());
        };
    }

    @Override
    public void setMapRating(@NotNull String mapId, @NotNull String playerId, @NotNull MapRating rating) {
        var reqBody = GSON.toJson(rating);
        var req = HttpRequest.newBuilder()
                .method("PUT", HttpRequest.BodyPublishers.ofString(reqBody))
                .uri(URI.create(url + "/" + mapId + "/ratings/" + playerId))
                .header(AUTHORIZER_HEADER, playerId)
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() == 200) return; // Ok
        throw new InternalError("Failed to set map rating (" + res.statusCode() + "): " + res.body());
    }

    @Override
    public @NotNull MapPlayerData getMapPlayerData(@NotNull String playerId) {
        var req = HttpRequest.newBuilder()
                .uri(URI.create(url + "/players/" + playerId))
                .header(AUTHORIZER_HEADER, playerId)
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() == 200) return GSON.fromJson(res.body(), MapPlayerData.class); // Ok
        throw new InternalError("Failed to get map player data: " + res.body());
    }

    @Override
    public @NotNull List<LegacyMapInfo> getLegacyMaps(@NotNull String authorizer, @NotNull String playerId) {
        var req = HttpRequest.newBuilder()
                .uri(URI.create(legacyUrl + "/" + playerId))
                .header(AUTHORIZER_HEADER, authorizer)
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 200 -> GSON.fromJson(res.body(), new TypeToken<List<LegacyMapInfo>>() {
            });
            case 404 -> List.of();
            default -> throw new InternalError("Failed to get legacy maps: " + res.body());
        };
    }

    @Override
    public @NotNull MapData.WithSlot importLegacyMap(@NotNull String authorizer, @NotNull String playerId, @NotNull String legacyMapId) {
        var req = HttpRequest.newBuilder()
                .method("POST", HttpRequest.BodyPublishers.noBody())
                .uri(URI.create(legacyUrl + "/" + playerId + "/" + legacyMapId + "/import"))
                .header(AUTHORIZER_HEADER, authorizer)
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 200 -> GSON.fromJson(res.body(), MapData.WithSlot.class);
            case 404 -> throw new MapService.NotFoundError(legacyMapId);
            case 403 -> throw new MapService.NoPermissionError();
            default -> throw new InternalError("Failed to import legacy map: " + res.body());
        };
    }

    @Override
    public void uploadPerfdump(@NotNull String name, @NotNull Path file) {
        try {
            var req = HttpRequest.newBuilder()
                    .method("PUT", HttpRequest.BodyPublishers.ofFile(file))
                    .uri(URI.create(perfdumpUrl + "/" + name))
                    .header(AUTHORIZER_HEADER, UUID.randomUUID().toString())
                    .build();
            var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 200) return; // Ok
            throw new InternalError("Failed to upload perfdump: " + res.body());
        } catch (FileNotFoundException e) {
            throw new InternalError(e);
        }
    }
}
