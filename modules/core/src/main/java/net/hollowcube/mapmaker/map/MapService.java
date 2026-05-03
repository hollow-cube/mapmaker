package net.hollowcube.mapmaker.map;

import net.hollowcube.mapmaker.map.requests.MapSearchParams;
import net.hollowcube.mapmaker.map.responses.PlayerTopTimesResponse;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.nio.channels.Channels;
import java.util.List;

@Blocking
public interface MapService {

    @NotNull
    String LEADERBOARD_TOP_TIMES = "top_times";
    @NotNull
    String LEADERBOARD_MAPS_BEATEN = "maps_beaten";

    @NotNull
    net.hollowcube.mapmaker.map.responses.MapSearchResponse searchMaps(@NotNull MapSearchParams request);

    @NotNull
    MapProgressBatchResponse getMapProgress(@NotNull String playerId, @NotNull List<String> mapIds);

    @NotNull
    MapData getMap(@NotNull String authorizer, @NotNull String id);

    @NotNull
    List<MapData> getMaps(@NotNull String authorizer, @NotNull List<String> mapIds);

    @NotNull
    MapData getMapByPublishedId(@NotNull String authorizer, long publishedId);

    void updateMap(@NotNull String authorizer, @NotNull String id, @NotNull MapUpdateRequest update);

    void deleteMap(@NotNull String authorizer, @NotNull String id, @Nullable String reason);

    void beginVerification(@NotNull String authorizer, @NotNull String mapId);

    void deleteVerification(@NotNull String authorizer, @NotNull String mapId);

    @NotNull
    MapData publishMap(@NotNull String authorizer, @NotNull String id);

    byte @Nullable [] getMapWorld(@NotNull String id, boolean write);

    default @Nullable ReadableMapData getMapWorldAsStream(@NotNull String id, boolean write) {
        var result = getMapWorld(id, write);
        return result == null ? null : new ReadableMapData(Channels.newChannel(new ByteArrayInputStream(result)), result.length);
    }

    void updateMapWorld(@NotNull String id, byte @NotNull [] worldData, long loadTime);

    void reportMap(@NotNull String mapId, @NotNull MapReportRequest req);

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

    @NotNull MapRating getMapRating(@NotNull String mapId, @NotNull String playerId);

    void setMapRating(@NotNull String mapId, @NotNull String playerId, @NotNull MapRating rating);

    @NotNull
    MapPlayerData getMapPlayerData(@NotNull String playerId);

    @NotNull
    MapHistory getPlayerMapHistory(@NotNull String playerId, int page, int amount);

    @NotNull
    PlayerTopTimesResponse getPlayerTopTimes(@NotNull String playerId, int page, int pageSize);

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
