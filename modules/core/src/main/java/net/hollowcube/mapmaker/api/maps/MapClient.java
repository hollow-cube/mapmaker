package net.hollowcube.mapmaker.api.maps;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import net.hollowcube.datafix.DataFixer;
import net.hollowcube.mapmaker.api.HttpClientWrapper;
import net.hollowcube.mapmaker.api.PaginatedList;
import net.hollowcube.mapmaker.api.ResultList;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.map.requests.MapSearchParams;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.minestom.server.MinecraftServer;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.registry.RegistryTranscoder;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

import static net.hollowcube.mapmaker.api.ApiClient.notImplemented;
import static net.hollowcube.mapmaker.api.HttpClientWrapper.query;

public interface MapClient {

    default MapData create(String owner, MapSize size) {
        throw notImplemented();
    }

    /// Get a map by its internal ID
    default MapData get(String mapId) {
        throw notImplemented();
    }

    default void update(String mapId, MapUpdateRequest body) {
        throw notImplemented();
    }

    default void delete(String actorId, String mapId, @Nullable String reason) {
        throw notImplemented();
    }

    default void publish(String mapId) {
        throw notImplemented();
    }

    default void beginVerification(String mapId) {
        throw notImplemented();
    }

    default void deleteVerification(String mapId) {
        throw notImplemented();
    }

    default ResultList<MapSlot> getPlayerSlots(String playerId) {
        throw notImplemented();
    }

    default ResultList<MapBuilder> getMapBuilders(String mapId, boolean onlyActive) {
        throw notImplemented();
    }

    default void inviteMapBuilder(String mapId, String playerId) {
        throw notImplemented();
    }

    default void removeMapBuilder(String mapId, String playerId) {
        throw notImplemented();
    }

    /// TODO: returns 400 if you dont have a slot, should have more specific handling for this.
    default void acceptMapBuilderInvite(String mapId, String playerId) {
        throw notImplemented();
    }

    default void rejectMapBuilderInvite(String mapId, String playerId) {
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

    default PaginatedList<String> getPlayerMapHistory(String playerId, int page, int pageSize) {
        throw notImplemented();
    }

    default PaginatedList<PlayerTopTimeEntry> getPlayerTopTimes(String playerId, int page, int pageSize) {
        throw notImplemented();
    }

    default PaginatedList<MapData> search(MapSearchParams params) {
        throw notImplemented();
    }

    default ResultList<PlayerMapProgress> searchMapProgress(String playerId, List<String> mapIds) {
        throw notImplemented();
    }

    default SaveState getLatestSaveState(String mapId, String playerId, @Nullable SaveStateType type, @Nullable SaveStateType.Serializer<?> serializer) {
        throw notImplemented();
    }

    default SaveState getBestSaveState(String mapId, String playerId) {
        throw notImplemented();
    }

    default void updateSaveState(String mapId, String playerId, String saveStateId, SaveStateUpdateRequest update) {
        throw notImplemented();
    }

    record Http(HttpClientWrapper http) implements MapClient {
        private static final String V4_PREFIX = "/v4/internal/maps";
        private static final String V4_PLAYERS_PREFIX = "/v4/internal/players";

        @Override
        public MapData create(String owner, MapSize size) {
            return http.post(
                "createMap",
                V4_PREFIX,
                Map.of("owner", owner, "size", size),
                new TypeToken<>() {});
        }

        @Override
        public MapData get(String mapId) {
            return http.get(
                "getMap",
                V4_PREFIX + "/" + mapId,
                new TypeToken<>() {});
        }

        @Override
        public void update(String mapId, MapUpdateRequest body) {
            http.patch(
                "updateMap",
                V4_PREFIX + "/" + mapId,
                body);
        }

        @Override
        public void delete(String actorId, String mapId, @Nullable String reason) {
            http.delete(
                "deleteMap",
                V4_PREFIX + "/" + mapId + query("actorId", actorId, "reason", reason)
            );
        }

        @Override
        public void publish(String mapId) {
            http.post(
                "publishMap",
                V4_PREFIX + "/" + mapId + "/publish"
            );
        }

        @Override
        public void beginVerification(String mapId) {
            http.post(
                "beginVerification",
                V4_PREFIX + "/" + mapId + "/verification"
            );
        }

        @Override
        public void deleteVerification(String mapId) {
            http.delete(
                "deleteVerification",
                V4_PREFIX + "/" + mapId + "/verification"
            );
        }

        @Override
        public ResultList<MapSlot> getPlayerSlots(String playerId) {
            return http.get(
                "getPlayerSlots",
                V4_PLAYERS_PREFIX + "/" + playerId + "/map-slots",
                new TypeToken<>() {});
        }

        @Override
        public ResultList<MapBuilder> getMapBuilders(String mapId, boolean onlyActive) {
            return http.get(
                "getMapBuilders",
                V4_PREFIX + "/" + mapId + "/builders" + query("onlyActive", onlyActive),
                new TypeToken<>() {});
        }

        @Override
        public void inviteMapBuilder(String mapId, String playerId) {
            http.post(
                "inviteMapBuilder",
                V4_PREFIX + "/" + mapId + "/builders",
                Map.of("playerId", playerId));
        }

        @Override
        public void removeMapBuilder(String mapId, String playerId) {
            http.delete(
                "removeMapBuilder",
                V4_PREFIX + "/" + mapId + "/builders/" + playerId);
        }

        @Override
        public void acceptMapBuilderInvite(String mapId, String playerId) {
            http.post(
                "acceptMapBuilderInvite",
                V4_PREFIX + "/" + mapId + "/builders/" + playerId + "/accept");
        }

        @Override
        public void rejectMapBuilderInvite(String mapId, String playerId) {
            http.post(
                "rejectMapBuilderInvite",
                V4_PREFIX + "/" + mapId + "/builders/" + playerId + "/reject");
        }

        @Override
        public void report(String mapId, MapReport report) {
            http.post(
                "reportMap",
                V4_PREFIX + "/" + mapId + "/reports",
                report
            );
        }

        @Override
        public MapRating getPlayerRating(String mapId, String playerId) {
            return http.get(
                "getMapPlayerRating",
                V4_PREFIX + "/" + mapId + "/ratings/" + playerId,
                new TypeToken<>() {}
            );
        }

        @Override
        public void setPlayerRating(String mapId, String playerId, MapRating rating) {
            http.post(
                "setMapPlayerRating",
                V4_PREFIX + "/" + mapId + "/ratings/" + playerId,
                Map.of("rating", rating)
            );
        }

        @Override
        public PaginatedList<String> getPlayerMapHistory(String playerId, int page, int pageSize) {
            return http.get(
                "getPlayerMapHistory",
                V4_PLAYERS_PREFIX + "/" + playerId + "/map-history" + query("page", page, "pageSize", pageSize),
                new TypeToken<>() {}
            );
        }

        @Override
        public PaginatedList<PlayerTopTimeEntry> getPlayerTopTimes(String playerId, int page, int pageSize) {
            return http.get(
                "getPlayerTopTimes",
                V4_PLAYERS_PREFIX + "/" + playerId + "/top-times" + query("page", page, "pageSize", pageSize),
                new TypeToken<>() {}
            );
        }

        @Override
        public PaginatedList<MapData> search(MapSearchParams params) {
            return http.get(
                "searchMaps",
                params.toUrl(V4_PREFIX + "/search"),
                new TypeToken<>() {}
            );
        }

        @Override
        public ResultList<PlayerMapProgress> searchMapProgress(String playerId, List<String> mapIds) {
            return http.post(
                "searchMapProgress",
                V4_PREFIX + "/search/progress",
                Map.of("playerId", playerId, "mapIds", mapIds),
                new TypeToken<>() {}
            );
        }

        @Override
        public SaveState getLatestSaveState(String mapId, String playerId, @Nullable SaveStateType type, @Nullable SaveStateType.Serializer<?> serializer) {
            JsonObject raw = http.get(
                "getLatestSaveState",
                V4_PREFIX + "/" + mapId + "/states/" + playerId + "/latest" + query(type == null ? null : type.name().toLowerCase()),
                new TypeToken<>() {}
            );

            var saveState = AbstractHttpService.GSON.fromJson(raw, SaveState.class);
            if (serializer != null) {
                var stateObj = raw.get(serializer.name()) instanceof JsonObject jo ? jo : new JsonObject();

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
        public SaveState getBestSaveState(String mapId, String playerId) {
            return http.get(
                "getLatestSaveState",
                V4_PREFIX + "/" + mapId + "/states/" + playerId + "/best",
                new TypeToken<>() {}
            );
        }

        @Override
        public void updateSaveState(String mapId, String playerId, String saveStateId, SaveStateUpdateRequest update) {
            http.put(
                "updateSaveState",
                V4_PREFIX + "/" + mapId + "/states/" + playerId + "/" + saveStateId,
                update.updates()
            );
        }

    }

    record Noop() implements MapClient {}
}
