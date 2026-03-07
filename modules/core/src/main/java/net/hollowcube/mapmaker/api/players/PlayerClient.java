package net.hollowcube.mapmaker.api.players;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import net.hollowcube.mapmaker.api.HttpClientWrapper;
import net.hollowcube.mapmaker.api.ResultList;
import net.hollowcube.mapmaker.player.DisplayName;
import net.hollowcube.mapmaker.player.PlayerData;

import java.util.List;
import java.util.Map;

import static net.hollowcube.mapmaker.api.ApiClient.notImplemented;

public interface PlayerClient {

    default PlayerData getPlayerData(String playerId) {
        throw notImplemented();
    }

    default DisplayName getDisplayName(String playerId) {
        throw notImplemented();
    }

    default void updatePlayerSettings(String playerId, JsonObject settings) {
        throw notImplemented();
    }

    default ResultList<PlayerDataStub> searchPlayers(String query, List<String> exclude, int limit) {
        throw notImplemented();
    }

    record Noop() implements PlayerClient {}

    record Http(HttpClientWrapper http) implements PlayerClient {
        private static final String V4_PREFIX = "/v4/internal/players";

        @Override
        public PlayerData getPlayerData(String playerId) {
            return http.get(
                "getPlayerData",
                V4_PREFIX + "/" + playerId,
                new TypeToken<>() {});
        }

        @Override
        public DisplayName getDisplayName(String playerId) {
            return http.get(
                "getDisplayName",
                V4_PREFIX + "/" + playerId + "/display-name",
                new TypeToken<>() {});
        }

        @Override
        public void updatePlayerSettings(String playerId, JsonObject settings) {
            http.patch(
                "updatePlayerSettings",
                V4_PREFIX + "/" + playerId,
                Map.of("settingsUpdates", settings));
        }

        @Override
        public ResultList<PlayerDataStub> searchPlayers(String query, List<String> exclude, int limit) {
            return http.post(
                "searchPlayers",
                V4_PREFIX + "/search",
                Map.of("query", query, "exclude", exclude, "limit", limit),
                new TypeToken<>() {});
        }

    }
}
