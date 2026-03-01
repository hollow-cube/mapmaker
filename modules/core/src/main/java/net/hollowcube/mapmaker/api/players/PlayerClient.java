package net.hollowcube.mapmaker.api.players;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import net.hollowcube.mapmaker.api.HttpClientWrapper;
import net.hollowcube.mapmaker.player.PlayerData;

import java.util.Map;

import static net.hollowcube.mapmaker.api.ApiClient.notImplemented;

public interface PlayerClient {

    default PlayerData getPlayerData(String playerId) {
        throw notImplemented();
    }

    default void updatePlayerSettings(String playerId, JsonObject settings) {
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
        public void updatePlayerSettings(String playerId, JsonObject settings) {
            http.patch(
                "updatePlayerSettings",
                V4_PREFIX + "/" + playerId,
                Map.of("settingsUpdates", settings)
            );
        }
    }

}
