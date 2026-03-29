package net.hollowcube.mapmaker.api.maps;

import com.google.gson.reflect.TypeToken;
import net.hollowcube.mapmaker.api.HttpClientWrapper;
import net.hollowcube.mapmaker.api.ResultList;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapSize;
import net.hollowcube.mapmaker.map.MapUpdateRequest;

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
    }

    record Noop() implements MapClient {}
}
