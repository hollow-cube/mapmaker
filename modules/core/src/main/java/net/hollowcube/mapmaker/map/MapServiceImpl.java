package net.hollowcube.mapmaker.map;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.datafix.DataFixer;
import net.hollowcube.mapmaker.map.requests.MapCreateRequest;
import net.hollowcube.mapmaker.map.requests.MapSearchParams;
import net.hollowcube.mapmaker.map.responses.PlayerTopTimesResponse;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.minestom.server.MinecraftServer;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.registry.RegistryTranscoder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
    public @NotNull MapData createMap(@NotNull MapCreateRequest request) {
        FutureUtil.assertThreadWarn();
        logger.log(System.Logger.Level.INFO, "creating new map for " + request.owner() + ", is for org: " + request.isOrg());

        var req = HttpRequest.newBuilder()
            .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(request)))
            .uri(URI.create(urlV3 + "/maps"))
            .header(AUTHORIZER_HEADER, request.authorizer())
            .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());

        return switch (res.statusCode()) {
            case 201 -> GSON.fromJson(res.body(), MapData.class);
            case 400 -> {
                var err = GSON.fromJson(res.body(), ErrorRes.class);
                if ("slot_in_use".equals(err.code)) throw new SlotInUseError();
                throw new InternalError("Failed to create map: " + err);
            }
            default -> throw new InternalError("Failed to create map: " + res.body());
        };
    }

    @Override
    public @NotNull net.hollowcube.mapmaker.map.responses.MapSearchResponse searchMaps(@NotNull MapSearchParams request) {
        var req = HttpRequest.newBuilder()
            .uri(URI.create(request.toUrl(urlV3 + "/maps/search")))
            .header(AUTHORIZER_HEADER, request.authorizer())
            .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) throw new InternalError("Failed to search maps: " + res.body());
        return GSON.fromJson(res.body(), net.hollowcube.mapmaker.map.responses.MapSearchResponse.class);
    }

    @Override
    public @NotNull MapProgressBatchResponse getMapProgress(@NotNull String playerId, @NotNull List<String> mapIds) {
        var req = HttpRequest.newBuilder()
            .uri(URI.create(urlV3 + "/maps/progress?playerId=" + playerId + "&mapIds=" + String.join(",", mapIds)))
            .header(AUTHORIZER_HEADER, playerId)
            .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 200 -> GSON.fromJson(res.body(), MapProgressBatchResponse.class);
            default -> throw new InternalError("Failed to search maps: " + res.body());
        };
    }

    @Override
    public @NotNull MapSearchResponse<MapData> searchOrgMaps(@NotNull String authorizer, int page, int pageSize, @NotNull String orgId) {
        String endpoint = url + "/search_orgs?" + "page=" + page + "&pageSize=" + pageSize + "&orgId=" + orgId;
        var req = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .header(AUTHORIZER_HEADER, authorizer)
            .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 200 -> GSON.fromJson(res.body(), new TypeToken<MapSearchResponse<MapData>>() {
            });
            default -> throw new InternalError("Failed to search maps: " + res.body());
        };
    }

    @Override
    public @NotNull MapData getMap(@NotNull String authorizer, @NotNull String id) {
        var req = HttpRequest.newBuilder()
            .uri(URI.create(urlV3 + "/maps/" + id))
            .header(AUTHORIZER_HEADER, authorizer)
            .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 200 -> GSON.fromJson(res.body(), MapData.class);
            case 404 -> throw new NotFoundError(id);
            default -> throw new InternalError("Failed to get map: " + res.body());
        };
    }

    /// ONLY returns published maps currently.
    @Override
    public @NotNull List<MapData> getMaps(@NotNull String authorizer, @NotNull List<String> mapIds) {
        if (mapIds.isEmpty()) return List.of();
        var req = HttpRequest.newBuilder()
            .uri(URI.create(urlV3 + "/maps?mapIds=" + String.join(",", mapIds)))
            .header(AUTHORIZER_HEADER, authorizer)
            .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 200 -> GSON.fromJson(res.body(), MapDataResults.class).results();
            default -> throw new InternalError("Failed to get maps: " + res.statusCode() + ": " + res.body());
        };
    }

    @Override
    public @NotNull MapData getMapByPublishedId(@NotNull String authorizer, long publishedId) {
        var req = HttpRequest.newBuilder()
            .uri(URI.create(urlV3 + "/maps/" + publishedId))
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
        var reqBody = GSON.toJson(update);
        var req = HttpRequest.newBuilder()
            .method("PATCH", HttpRequest.BodyPublishers.ofString(reqBody))
            .uri(URI.create(urlV3 + "/maps/" + id))
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
    public void deleteMap(@NotNull String authorizer, @NotNull String id, @Nullable String reason) {
        var body = new HashMap<String, String>();
        body.put("reason", reason);
        var reqBody = GSON.toJson(body);
        var req = HttpRequest.newBuilder()
            .method("DELETE", HttpRequest.BodyPublishers.ofString(reqBody))
            .uri(URI.create(urlV3 + "/maps/" + id))
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
        var req = HttpRequest.newBuilder()
            .POST(HttpRequest.BodyPublishers.noBody())
            .uri(URI.create(urlV3 + "/maps/" + mapId + "/verify"))
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
        var req = HttpRequest.newBuilder()
            .method("DELETE", HttpRequest.BodyPublishers.noBody())
            .uri(URI.create(urlV3 + "/maps/" + mapId + "/verify"))
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
        var body = "{}";
        var req = HttpRequest.newBuilder()
            .method("POST", HttpRequest.BodyPublishers.ofString(body))
            .uri(URI.create(urlV3 + "/maps/" + id + "/publish"))
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
    public void updateMapWorld(@NotNull String id, byte @NotNull [] worldData) {
        logger.log(System.Logger.Level.INFO, "Updating map world for " + id + ", length: " + worldData.length);
        var req = HttpRequest.newBuilder()
            .method("PUT", HttpRequest.BodyPublishers.ofByteArray(worldData))
            .uri(URI.create(urlV3 + "/maps/" + id + "/world"))
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
    public void reportMap(@NotNull String mapId, @NotNull MapReportRequest req) {
        var reqBody = GSON.toJson(req);
        var req2 = HttpRequest.newBuilder()
            .method("POST", HttpRequest.BodyPublishers.ofString(reqBody))
            .uri(URI.create(urlV3 + "/maps/" + mapId + "/report"))
            .header(AUTHORIZER_HEADER, UUID.randomUUID().toString()) //todo
            .build();
        var res = doRequest(req2, HttpResponse.BodyHandlers.ofString());
        switch (res.statusCode()) {
            case 200 -> {
            }
            default -> throw new InternalError("Failed to report map: " + res.body());
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
    public void deletePlaytimeLeaderboard(@NotNull String authorizer, @NotNull String mapId, @Nullable String playerId) {
        var uri = urlV3 + "/maps/" + mapId + "/leaderboard/playtime";
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

    @Override
    public @NotNull SaveState createSaveState(@NotNull String mapId, @NotNull String playerId, int protocolVersion, @Nullable SaveStateType.Serializer<?> serializer) {
        var req = HttpRequest.newBuilder()
            .method("POST", HttpRequest.BodyPublishers.ofString("""
                {"protocolVersion": %d}
                """.formatted(protocolVersion)))
            .uri(URI.create(urlV3 + "/maps/" + mapId + "/savestates/" + playerId))
            .header(AUTHORIZER_HEADER, UUID.randomUUID().toString()) //todo
            .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 201 -> readTypedSaveState(GSON.fromJson(res.body(), JsonObject.class), serializer);
            default -> throw new InternalError("Failed to create savestate: " + res.body());
        };
    }

    @Override
    public @NotNull SaveState getLatestSaveState(@NotNull String mapId, @NotNull String playerId, @Nullable SaveStateType type, @Nullable SaveStateType.Serializer<?> serializer) {
        var req = HttpRequest.newBuilder()
            .uri(URI.create(urlV3 + "/maps/" + mapId + "/savestates/" + playerId + "/latest?typeFilter=" + (type == null ? "" : type.name().toLowerCase(Locale.ROOT))))
            .header(AUTHORIZER_HEADER, UUID.randomUUID().toString()) //todo
            .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 200 -> readTypedSaveState(GSON.fromJson(res.body(), JsonObject.class), serializer);
            case 404 -> throw new NotFoundError("latest");
            default -> throw new InternalError("Failed to get latest savestate: " + res.body());
        };
    }

    private @NotNull SaveState readTypedSaveState(@NotNull JsonObject obj, @Nullable SaveStateType.Serializer<?> serializer) {
        var saveState = GSON.fromJson(obj, SaveState.class);
        if (serializer != null) {
            var stateObj = obj.get(serializer.name()) instanceof JsonObject jo ? jo : new JsonObject();

            // Upgrade the save state if relevant
            // Note that this is a non-backwards compatible change, so once we write a new state an old server cannot necessarily
            // read this state. For now, we will likely ignore this, however in the future joining a map will require checking
            // the state and finding a compatible server (server data version > state data version).
            if (!stateObj.isEmpty() && saveState.dataVersion < DataFixer.maxVersion()) {
                var upgraded = DataFixer.upgrade(serializer.dataType(), Transcoder.JSON, stateObj, saveState.dataVersion, DataFixer.maxVersion());
                if (!(upgraded instanceof JsonObject upgradedObject))
                    throw new IllegalStateException("invalid save state upgrade: " + upgraded);
                stateObj = upgradedObject;
                saveState.dataVersion = DataFixer.maxVersion();
            }

            saveState.serializer = serializer;
            var coder = new RegistryTranscoder<>(Transcoder.JSON, MinecraftServer.process());
            saveState.state = serializer.codec().decode(coder, stateObj).orElseThrow();
        }
        return saveState;
    }

    @Override
    public @Nullable SaveState getBestSaveState(@NotNull String mapId, @NotNull String playerId) {
        var req = HttpRequest.newBuilder()
            .uri(URI.create(urlV3 + "/maps/" + mapId + "/savestates/" + playerId + "/best"))
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
    public @Nullable SaveStateUpdateResponse updateSaveState(@NotNull String mapId, @NotNull String playerId, @NotNull String id, @NotNull SaveStateUpdateRequest update) {
        update.updates.addProperty("dataVersion", DataFixer.maxVersion());
        var reqBody = GSON.toJson(update.updates);
        var req = HttpRequest.newBuilder()
            .method("PATCH", HttpRequest.BodyPublishers.ofString(reqBody))
            .uri(URI.create(urlV3 + "/maps/" + mapId + "/savestates/" + playerId + "/" + id))
            .header(AUTHORIZER_HEADER, UUID.randomUUID().toString()) //todo
            .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 200 -> {
                if (res.body().isEmpty()) yield null;
                else yield GSON.fromJson(res.body(), SaveStateUpdateResponse.class);
            }
            case 404 -> throw new NotFoundError(id);
            default -> throw new InternalError("Failed to update savestate (" + res.statusCode() + "): " + res.body());
        };
    }

    @Override
    public void deleteSaveState(@NotNull String mapId, @NotNull String playerId, @NotNull String id) {
        var req = HttpRequest.newBuilder()
            .method("DELETE", HttpRequest.BodyPublishers.noBody())
            .uri(URI.create(urlV3 + "/maps/" + mapId + "/savestates/" + playerId + "/" + id))
            .header(AUTHORIZER_HEADER, UUID.randomUUID().toString()) //todo
            .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() == 204) return; // Ok
        throw new InternalError("Failed to delete savestate: " + res.body());
    }

    @Override
    public @NotNull MapRating getMapRating(@NotNull String mapId, @NotNull String playerId) {
        var req = HttpRequest.newBuilder()
            .uri(URI.create(urlV3 + "/maps/" + mapId + "/ratings/" + playerId))
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
            .uri(URI.create(urlV3 + "/maps/" + mapId + "/ratings/" + playerId))
            .header(AUTHORIZER_HEADER, playerId)
            .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() == 200) return; // Ok
        throw new InternalError("Failed to set map rating (" + res.statusCode() + "): " + res.body());
    }

    @Override
    public @NotNull MapPlayerData getMapPlayerData(@NotNull String playerId) {
        var req = HttpRequest.newBuilder()
            .uri(URI.create(urlV3 + "/map-players/" + playerId))
            .header(AUTHORIZER_HEADER, playerId)
            .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() == 200) return GSON.fromJson(res.body(), MapPlayerData.class); // Ok
        throw new InternalError("Failed to get map player data: " + res.body());
    }

    @Override
    public @NotNull MapHistory getPlayerMapHistory(@NotNull String playerId, int page, int amount) {
        var req = HttpRequest.newBuilder()
            .uri(URI.create(urlV3 + "/map-players/" + playerId + "/history?page=" + page + "&pageSize=" + amount))
            .header(AUTHORIZER_HEADER, playerId)
            .build();

        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 200 -> GSON.fromJson(res.body(), MapHistory.class);
            case 404 -> new MapHistory(page, false, List.of());
            default -> throw new InternalError("Failed to get player map history: " + res.body());
        };
    }

    @Override
    public @NotNull PlayerTopTimesResponse getPlayerTopTimes(@NotNull String playerId, int page, int pageSize) {
        var req = HttpRequest.newBuilder()
            .uri(URI.create("%s/map-players/%s/topTimes?page=%s&pageSize=%s".formatted(urlV3, playerId, page, pageSize)))
            .GET()
            .build();

        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 200 -> GSON.fromJson(res.body(), PlayerTopTimesResponse.class);
            default -> throw new InternalError("Failed to get player top times: " + res.body());
        };
    }

}
