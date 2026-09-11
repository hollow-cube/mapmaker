package net.hollowcube.mapmaker.api.maps;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import net.hollowcube.datafix.DataFixer;
import net.hollowcube.ipc.Blob;
import net.hollowcube.ipc.map.BeginVerificationResult;
import net.hollowcube.ipc.map.BuilderResult;
import net.hollowcube.ipc.map.CreateMapResult;
import net.hollowcube.ipc.map.DeleteVerificationResult;
import net.hollowcube.ipc.map.MapBuilder;
import net.hollowcube.ipc.map.MapData;
import net.hollowcube.ipc.map.MapPatch;
import net.hollowcube.ipc.map.MapReportCategory;
import net.hollowcube.ipc.map.MapService;
import net.hollowcube.ipc.map.MapSize;
import net.hollowcube.ipc.map.MapSlot;
import net.hollowcube.ipc.map.MapStatus;
import net.hollowcube.ipc.map.PublishMapResult;
import net.hollowcube.ipc.map.SaveStateType;
import net.hollowcube.ipc.map.SaveStateUpdate;
import net.hollowcube.ipc.util.IpcException;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.api.HttpClientWrapper;
import net.hollowcube.mapmaker.api.ResultList;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.minestom.server.MinecraftServer;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.registry.RegistryTranscoder;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.Channels;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static net.hollowcube.mapmaker.api.ApiClient.notImplemented;
import static net.hollowcube.mapmaker.api.HttpClientWrapper.query;

public interface MapClient {

    default CreateMapResult create(String owner, MapSize size) {
        throw notImplemented();
    }

    /// Get a map by its internal ID
    default MapData get(String mapId) {
        throw notImplemented();
    }

    default void update(String mapId, MapPatch body) {
        throw notImplemented();
    }

    default void delete(String actorId, String mapId, @Nullable String reason) {
        throw notImplemented();
    }

    default byte[] getWorld(String mapId) {
        throw notImplemented();
    }

    default ReadableMapData getWorldStream(String id) {
        // Default impl just wraps the byte array so not actually streaming. Polar stream loader doesnt benefit
        // much from streaming, but we can replace this implementation in the future if theres a benefit.
        var bytes = getWorld(id);
        return new ReadableMapData(Channels.newChannel(new ByteArrayInputStream(bytes)), bytes.length);
    }

    default void updateWorld(String mapId, byte[] worldData, long loadTime) {
        throw notImplemented();
    }

    default MapStatus getStatus(String mapId) {
        throw notImplemented();
    }

    default PublishMapResult publish(String mapId) {
        throw notImplemented();
    }

    default BeginVerificationResult beginVerification(String mapId) {
        throw notImplemented();
    }

    default DeleteVerificationResult deleteVerification(String mapId) {
        throw notImplemented();
    }

    default ResultList<MapSlot> getPlayerSlots(String playerId) {
        throw notImplemented();
    }

    default ResultList<MapBuilder> getMapBuilders(String mapId, boolean onlyActive) {
        throw notImplemented();
    }

    default BuilderResult inviteMapBuilder(String mapId, String playerId) {
        throw notImplemented();
    }

    default BuilderResult removeMapBuilder(String mapId, String playerId) {
        throw notImplemented();
    }

    default BuilderResult acceptMapBuilderInvite(String mapId, String playerId) {
        throw notImplemented();
    }

    default BuilderResult rejectMapBuilderInvite(String mapId, String playerId) {
        throw notImplemented();
    }

    default void report(String mapId, MapReport report) {
        throw notImplemented();
    }

    default MapRating getPlayerRating(String mapId, String playerId) {
        throw notImplemented();
    }

    default void setPlayerRating(String mapId, String playerId, MapRating rating) {
        throw notImplemented();
    }

    /// The newest state of this type, decoded with `serializer` when given; [ApiClient.NotFoundError] when
    /// there is none to continue from.
    default SaveState getLatestSaveState(String mapId, String playerId, SaveStateType type, @Nullable SaveState.Serializer<?> serializer) {
        throw notImplemented();
    }

    default SaveState getBestSaveState(String mapId, String playerId) {
        throw notImplemented();
    }

    default void updateSaveState(String mapId, String playerId, String saveStateId, SaveStateUpdate update) {
        throw notImplemented();
    }

    // Map files

    default ResultList<FileHeader> listMapFiles(String mapId) {
        throw notImplemented();
    }

    default byte @Nullable [] getMapFile(String mapId, String path) {
        throw notImplemented();
    }

    record Http(HttpClientWrapper http, MapService maps) implements MapClient {
        private static final String V4_PREFIX = "/v4/internal/maps";
        private static final String V4_PLAYERS_PREFIX = "/v4/internal/players";

        @Override
        public CreateMapResult create(String owner, MapSize size) {
            return maps.create(UUID.fromString(owner), size, MinecraftServer.PROTOCOL_VERSION);
        }

        @Override
        public MapData get(String mapId) {
            var map = maps.get(mapId);
            if (map == null) throw new ApiClient.NotFoundError();
            return map;
        }

        @Override
        public void update(String mapId, MapPatch body) {
            maps.update(UUID.fromString(mapId), body);
        }

        @Override
        public void delete(String actorId, String mapId, @Nullable String reason) {
            maps.delete(UUID.fromString(actorId), UUID.fromString(mapId), reason);
        }

        @Override
        public byte[] getWorld(String mapId) {
            try {
                return world(mapId).readAllBytes();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public ReadableMapData getWorldStream(String mapId) {
            var blob = world(mapId);
            return new ReadableMapData(Channels.newChannel(blob.stream()), blob.length());
        }

        private Blob world(String mapId) {
            try {
                return maps.getWorld(UUID.fromString(mapId));
            } catch (IpcException e) {
                if (e.status() == 404) throw new ApiClient.NotFoundError();
                throw e;
            }
        }

        @Override
        public void updateWorld(String mapId, byte[] bytes, long loadTime) {
            maps.updateWorld(UUID.fromString(mapId), loadTime, Blob.of(bytes));
        }

        @Override
        public MapStatus getStatus(String mapId) {
            return maps.getStatus(UUID.fromString(mapId));
        }

        @Override
        public PublishMapResult publish(String mapId) {
            return maps.publish(UUID.fromString(mapId));
        }

        @Override
        public BeginVerificationResult beginVerification(String mapId) {
            return maps.beginVerification(UUID.fromString(mapId));
        }

        @Override
        public DeleteVerificationResult deleteVerification(String mapId) {
            return maps.deleteVerification(UUID.fromString(mapId));
        }

        @Override
        public ResultList<MapSlot> getPlayerSlots(String playerId) {
            return new ResultList<>(maps.getPlayerSlots(UUID.fromString(playerId)));
        }

        @Override
        public ResultList<MapBuilder> getMapBuilders(String mapId, boolean onlyActive) {
            return new ResultList<>(maps.getBuilders(UUID.fromString(mapId), onlyActive));
        }

        @Override
        public BuilderResult inviteMapBuilder(String mapId, String playerId) {
            return maps.inviteBuilder(UUID.fromString(mapId), UUID.fromString(playerId));
        }

        @Override
        public BuilderResult removeMapBuilder(String mapId, String playerId) {
            return maps.removeBuilder(UUID.fromString(mapId), UUID.fromString(playerId));
        }

        @Override
        public BuilderResult acceptMapBuilderInvite(String mapId, String playerId) {
            return maps.acceptBuilderInvite(UUID.fromString(mapId), UUID.fromString(playerId));
        }

        @Override
        public BuilderResult rejectMapBuilderInvite(String mapId, String playerId) {
            return maps.rejectBuilderInvite(UUID.fromString(mapId), UUID.fromString(playerId));
        }

        @Override
        public void report(String mapId, MapReport report) {
            var categories = report.categories().stream()
                .map(category -> MapReportCategory.valueOf(category.name()))
                .toList();
            maps.report(UUID.fromString(mapId), UUID.fromString(report.reporter()), categories, report.comment());
        }

        @Override
        public MapRating getPlayerRating(String mapId, String playerId) {
            var state = switch (maps.getPlayerRating(UUID.fromString(mapId), UUID.fromString(playerId))) {
                case LIKED -> MapRating.State.LIKED;
                case DISLIKED -> MapRating.State.DISLIKED;
                case UNRATED, UNKNOWN -> MapRating.State.UNRATED;
            };
            return new MapRating(state, null);
        }

        @Override
        public void setPlayerRating(String mapId, String playerId, MapRating rating) {
            var value = switch (rating.state()) {
                case LIKED -> net.hollowcube.ipc.map.MapRating.LIKED;
                case DISLIKED -> net.hollowcube.ipc.map.MapRating.DISLIKED;
                case UNRATED -> net.hollowcube.ipc.map.MapRating.UNRATED;
            };
            maps.setPlayerRating(UUID.fromString(mapId), UUID.fromString(playerId), value);
        }

        @Override
        public SaveState getLatestSaveState(String mapId, String playerId, SaveStateType type, @Nullable SaveState.Serializer<?> serializer) {
            var data = maps.getLatestSaveState(UUID.fromString(mapId), UUID.fromString(playerId), type);
            if (data == null) throw new ApiClient.NotFoundError();
            if (serializer == null) return new SaveState(data, data.dataVersion(), null, null);

            var stateObj = data.state() != null ? data.state() : new JsonObject();
            var dataVersion = data.dataVersion();
            // Upgrade the save state if relevant
            // Note that this is a non-backwards compatible change, so once we write a new state an old server cannot necessarily
            // read this state. For now, we will likely ignore this, however in the future joining a map will require checking
            // the state and finding a compatible server (server data version > state data version).
            if (!stateObj.isEmpty() && dataVersion < DataFixer.maxVersion()) {
                var upgraded = DataFixer.upgrade(serializer.dataType(), Transcoder.JSON, stateObj, dataVersion, DataFixer.maxVersion());
                if (!(upgraded instanceof JsonObject upgradedObject))
                    throw new IllegalStateException("invalid save state upgrade: " + upgraded);
                stateObj = upgradedObject;
                dataVersion = DataFixer.maxVersion();
            }

            var coder = new RegistryTranscoder<>(Transcoder.JSON, MinecraftServer.process());
            var state = serializer.codec().decode(coder, stateObj).orElseThrow();
            return new SaveState(data, dataVersion, serializer, state);
        }

        @Override
        public SaveState getBestSaveState(String mapId, String playerId) {
            var data = maps.getBestSaveState(UUID.fromString(mapId), UUID.fromString(playerId));
            if (data == null) throw new ApiClient.NotFoundError();
            return new SaveState(data, data.dataVersion(), null, null);
        }

        @Override
        public void updateSaveState(String mapId, String playerId, String saveStateId, SaveStateUpdate update) {
            maps.upsertSaveState(UUID.fromString(mapId), UUID.fromString(playerId), UUID.fromString(saveStateId), update);
        }

        @Override
        public ResultList<FileHeader> listMapFiles(String mapId) {
            return http.get(
                "listMapFiles",
                V4_PREFIX + "/" + mapId + "/files",
                new TypeToken<>() {}
            );
        }

        @Override
        public byte @Nullable [] getMapFile(String mapId, String path) {
            var res = http.doRequest(
                "getMapFile",
                HttpRequest.newBuilder()
                    .uri(http.url(V4_PREFIX + "/" + mapId + "/files/" + path))
                    .GET(),
                HttpResponse.BodyHandlers.ofByteArray());
            if (res.statusCode() == 404) {
                return null;
            }
            http.maybeThrowResponse(res);
            return res.body();
        }
    }

    record Noop() implements MapClient {}
}
