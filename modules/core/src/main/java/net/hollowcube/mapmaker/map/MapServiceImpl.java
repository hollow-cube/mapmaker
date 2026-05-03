package net.hollowcube.mapmaker.map;

import com.google.gson.JsonObject;
import net.hollowcube.datafix.DataFixer;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.minestom.server.MinecraftServer;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.registry.RegistryTranscoder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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

}
