package net.hollowcube.mapmaker.map;

import net.hollowcube.mapmaker.util.AbstractHttpService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

public class MapServiceImpl extends AbstractHttpService implements MapService {
    private static final System.Logger logger = System.getLogger(MapServiceImpl.class.getName());

    public static final String AUTHORIZER_HEADER = "x-hc-user-id";
    private static final String WORLD_SESSION_HEADER = "x-hc-map-session";

    private static final String POLAR_CONTENT_TYPE = "application/vnd.hollowcube.polar";

    record ErrorRes(String code) {
    }

    private final String url;
    private final String urlV3;

    public MapServiceImpl(String url) {
        this.url = String.format("%s/v1/internal/maps", url);
        this.urlV3 = String.format("%s/v3/internal", url);
    }

    @Override
    public byte @Nullable [] getMapWorld(@NotNull String id, boolean write) {
        var req = HttpRequest.newBuilder()
            .uri(URI.create(urlV3 + "/maps/" + id + "/world?scope=" + (write ? "write" : "read")))
            .header(AUTHORIZER_HEADER, UUID.randomUUID().toString()) //todo
            .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofByteArray());
        return switch (res.statusCode()) {
            case 200 -> {
                logger.log(System.Logger.Level.INFO, "Received map world for " + id + ", length: " + res.body().length);
                yield res.body();
            }
            case 204 -> null;
            case 404 -> throw new NotFoundError(id);
            default -> throw new InternalError("Failed to get map world: " + new String(res.body()));
        };
    }

//    @Override
//    public @Nullable ReadableMapData getMapWorldAsStream(@NotNull String id, boolean write) {
//        var req = HttpRequest.newBuilder()
//                .uri(URI.create(url + "/" + id + "/world?scope=" + (write ? "write" : "read")))
//                .header(AUTHORIZER_HEADER, UUID.randomUUID().toString()) //todo
//                .build();
//        var res = doRequest(req, HttpResponse.BodyHandlers.ofInputStream());
//        return switch (res.statusCode()) {
//            case 200 -> {
//                OptionalLong contentLength = res.headers().firstValueAsLong("content-length");
//                if (contentLength.isEmpty()) {
//                    // Unknown content length so we need to read the entire body here.
//                    try (var bodyStream = res.body()) {
//                        byte[] body = bodyStream.readAllBytes();
//                        yield new ReadableMapData(Channels.newChannel(new ByteArrayInputStream(body)), body.length);
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }
//                }
//                yield new ReadableMapData(Channels.newChannel(res.body()), contentLength.getAsLong());
//            }
//            case 204 -> null;
//            case 404 -> throw new NotFoundError(id);
//            default -> {
//                try {
//                    throw new InternalError("Failed to get map world: " + new String(res.body().readAllBytes()));
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        };
//    }

    @Override
    public void updateMapWorld(@NotNull String id, byte @NotNull [] worldData, long loadTime) {
        logger.log(System.Logger.Level.INFO, "Updating map world for " + id + ", length: " + worldData.length);
        var req = HttpRequest.newBuilder()
            .method("PUT", HttpRequest.BodyPublishers.ofByteArray(worldData))
            .uri(URI.create(urlV3 + "/maps/" + id + "/world?loadTime=" + loadTime))
            .header(AUTHORIZER_HEADER, UUID.randomUUID().toString()) //todo
            .header(WORLD_SESSION_HEADER, "todo")
            .header("content-type", POLAR_CONTENT_TYPE)
            .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        switch (res.statusCode()) {
            case 200 -> {/* update ok */}
            case 404 -> throw new NotFoundError(id);
            default -> throw new InternalError("Failed to update map world: " + res.body());
        }
    }

    @Override
    public @NotNull LeaderboardData getGlobalLeaderboard(@NotNull String name, @Nullable String playerId) {
        var uri = urlV3 + "/maps/hub/leaderboard/" + name;
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
    public @NotNull LeaderboardData getPlaytimeLeaderboard(@NotNull String mapId, @Nullable String playerId) {
        var uri = urlV3 + "/maps/" + mapId + "/leaderboard/playtime";
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
    public void deletePlaytimeLeaderboard(@NotNull String authorizer, @NotNull String mapId, @Nullable String playerId, boolean notify) {
        var uri = urlV3 + "/maps/" + mapId + "/leaderboard/playtime";
        if (playerId != null) {
            uri += "?playerId=" + playerId;
            if (notify) uri += "&notify=true";
        }
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
        var uri = urlV3 + "/maps/" + mapId + "/leaderboard/playtime/restore";
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

}
