package net.hollowcube.mapmaker.map;

import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.nio.channels.Channels;

@Blocking
public interface MapService {

    @NotNull
    String LEADERBOARD_TOP_TIMES = "top_times";
    @NotNull
    String LEADERBOARD_MAPS_BEATEN = "maps_beaten";

    byte @Nullable [] getMapWorld(@NotNull String id, boolean write);

    default @Nullable ReadableMapData getMapWorldAsStream(@NotNull String id, boolean write) {
        var result = getMapWorld(id, write);
        return result == null ? null : new ReadableMapData(Channels.newChannel(new ByteArrayInputStream(result)), result.length);
    }

    void updateMapWorld(@NotNull String id, byte @NotNull [] worldData, long loadTime);

    @NotNull
    LeaderboardData getGlobalLeaderboard(@NotNull String name, @Nullable String playerId);

    @NotNull
    LeaderboardData getPlaytimeLeaderboard(@NotNull String mapId, @Nullable String playerId);

    void deletePlaytimeLeaderboard(@NotNull String authorizer, @NotNull String mapId, @Nullable String playerId, boolean notify);

    void restorePlaytimeLeaderboard(@NotNull String authorizer, @NotNull String mapId);

    // Save states

    @NotNull
    SaveState getLatestSaveState(@NotNull String mapId, @NotNull String playerId, @Nullable SaveStateType type, @Nullable SaveStateType.Serializer<?> serializer);

    @Nullable
    SaveState getBestSaveState(@NotNull String mapId, @NotNull String playerId);

    @Nullable
    SaveStateUpdateResponse updateSaveState(@NotNull String mapId, @NotNull String playerId, @NotNull String id, @NotNull SaveStateUpdateRequest update);

    class NotFoundError extends RuntimeException {
        public NotFoundError(@NotNull String id) {
            super("Map not found: " + id);
        }
    }

    class InternalError extends RuntimeException {
        public InternalError(@NotNull String message) {
            super(message);
        }

        public InternalError(Throwable cause) {
            super(cause);
        }
    }

}
