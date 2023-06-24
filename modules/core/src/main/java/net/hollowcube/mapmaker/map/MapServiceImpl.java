package net.hollowcube.mapmaker.map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.hollowcube.mapmaker.util.gson.InstantTypeAdapter;
import net.hollowcube.mapmaker.util.gson.MaterialTypeAdapter;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class MapServiceImpl implements MapService {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
            .registerTypeAdapter(Material.class, new MaterialTypeAdapter())
            .create();

    private static final String AUTHORIZER_HEADER = "x-hc-user-id";
    private static final String WORLD_SESSION_HEADER = "x-hc-map-session";

    private static final String POLAR_CONTENT_TYPE = "application/vnd.hollowcube.polar";

    private static final System.Logger logger = System.getLogger(MapServiceImpl.class.getName());

    private final HttpClient httpClient;
    private final String url;

    public MapServiceImpl(String url) {
        this.url = String.format("%s/v1/internal/maps", url);
        this.httpClient = HttpClient.newHttpClient();
    }


    @Override
    public @NotNull MapData createMap(@NotNull String authorizer, @NotNull String owner) {
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
    public @NotNull MapData getMap(@NotNull String authorizer, @NotNull String id) {
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
    public @NotNull SaveStateV2 createSaveState(@NotNull String mapId, @NotNull String playerId) {
        var req = HttpRequest.newBuilder()
                .method("POST", HttpRequest.BodyPublishers.noBody())
                .uri(URI.create(url + "/" + mapId + "/savestates/" + playerId))
                .header(AUTHORIZER_HEADER, UUID.randomUUID().toString()) //todo
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 201 -> GSON.fromJson(res.body(), SaveStateV2.class);
            default -> throw new InternalError("Failed to create savestate: " + res.body());
        };
    }

    @Override
    public @NotNull SaveStateV2 getSaveState(@NotNull String mapId, @NotNull String playerId, @NotNull String id) {
        var req = HttpRequest.newBuilder()
                .uri(URI.create(url + "/" + mapId + "/savestates/" + playerId + "/" + id))
                .header(AUTHORIZER_HEADER, UUID.randomUUID().toString()) //todo
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 200 -> GSON.fromJson(res.body(), SaveStateV2.class);
            case 404 -> throw new NotFoundError(id);
            default -> throw new InternalError("Failed to get savestate: " + res.body());
        };
    }

    @Override
    public @NotNull SaveStateV2 getLatestSaveState(@NotNull String mapId, @NotNull String playerId) {
        var req = HttpRequest.newBuilder()
                .uri(URI.create(url + "/" + mapId + "/savestates/" + playerId + "/latest"))
                .header(AUTHORIZER_HEADER, UUID.randomUUID().toString()) //todo
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 200 -> GSON.fromJson(res.body(), SaveStateV2.class);
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

    private <T> HttpResponse<T> doRequest(@NotNull HttpRequest req, HttpResponse.BodyHandler<T> handler) {
        try {
            var res = httpClient.send(req, handler);
            if (res.statusCode() == 403) {
                // We simply convert auth issues to 404s
                logger.log(System.Logger.Level.ERROR, "auth failed for request: " + req.method() + " " + req.uri());
                throw new NotFoundError("???");
            }
            return res;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalError(e);
        } catch (IOException e) {
            throw new InternalError(e);
        }
    }
}
