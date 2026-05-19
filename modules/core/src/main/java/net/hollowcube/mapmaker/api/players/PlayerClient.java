package net.hollowcube.mapmaker.api.players;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.api.HttpClientWrapper;
import net.hollowcube.mapmaker.api.ResultList;
import net.hollowcube.mapmaker.player.DisplayName;
import net.hollowcube.mapmaker.player.PlayerData;
import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static net.hollowcube.mapmaker.api.ApiClient.notImplemented;

public interface PlayerClient {

    default PlayerData getPlayerData(String playerId) {
        throw notImplemented();
    }

    default DisplayName getDisplayName(String playerId) {
        throw notImplemented();
    }

    default @Nullable Hypercube getHypercube(String playerId) {
        throw notImplemented();
    }

    default void updatePlayerSettings(String playerId, JsonObject settings) {
        throw notImplemented();
    }

    default ResultList<PlayerDataStub> searchPlayers(String query, List<String> exclude, int limit) {
        throw notImplemented();
    }

    default ResultList<PlayerDataStub> getAlts(String playerId) {
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
            var player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(UUID.fromString(playerId));
            if (player != null) return PlayerData.fromPlayer(player).displayName2();

            return http.get(
                "getDisplayName",
                V4_PREFIX + "/" + playerId + "/display-name",
                new TypeToken<>() {});
        }

        @Override
        public @Nullable Hypercube getHypercube(String playerId) {
            try {
                return http.get(
                    "getHypercube",
                    V4_PREFIX + "/" + playerId + "/hypercube",
                    new TypeToken<>() {});
            } catch (ApiClient.NotFoundError _) {
                return null;
            }
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

        @Override
        public ResultList<PlayerDataStub> getAlts(String playerId) {
            return http.get(
                "getAlts",
                V4_PREFIX + "/" + playerId + "/alts",
                new TypeToken<>() {});
        }

    }
}
