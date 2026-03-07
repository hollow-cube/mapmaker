package net.hollowcube.mapmaker.api.maps;

import com.google.gson.reflect.TypeToken;
import net.hollowcube.mapmaker.api.HttpClientWrapper;
import net.hollowcube.mapmaker.api.ResultList;

import static net.hollowcube.mapmaker.api.ApiClient.notImplemented;

public interface MapClient {

    default ResultList<MapSlot> getPlayerSlots(String playerId) {
        throw notImplemented();
    }

    record Http(HttpClientWrapper http) implements MapClient {
        private static final String V4_PLAYERS_PREFIX = "/v4/internal/players";

        @Override
        public ResultList<MapSlot> getPlayerSlots(String playerId) {
            return http.get(
                "getPlayerSlots",
                V4_PLAYERS_PREFIX + "/" + playerId + "/map-slots",
                new TypeToken<>() {});
        }

    }

    record Noop() implements MapClient {}
}
