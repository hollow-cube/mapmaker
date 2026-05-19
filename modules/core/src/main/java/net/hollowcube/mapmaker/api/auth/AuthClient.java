package net.hollowcube.mapmaker.api.auth;

import com.google.gson.reflect.TypeToken;
import net.hollowcube.mapmaker.api.HttpClientWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static net.hollowcube.mapmaker.api.ApiClient.notImplemented;

public interface AuthClient {

    default LaunchGrant createLaunchGrant(String playerId, @Nullable String mapId) {
        throw notImplemented();
    }

    record Noop() implements AuthClient {}

    record Http(HttpClientWrapper http) implements AuthClient {
        private static final String V4_PREFIX = "/v4/internal/auth";

        @Override
        public LaunchGrant createLaunchGrant(String playerId, @Nullable String mapId) {
            return http.post(
                "createLaunchGrant",
                V4_PREFIX + "/grant",
                Map.of("playerId", playerId, "mapId", mapId),
                new TypeToken<>() {}
            );
        }
    }
}
