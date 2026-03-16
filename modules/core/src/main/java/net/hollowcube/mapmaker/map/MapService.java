package net.hollowcube.mapmaker.map;

import net.hollowcube.mapmaker.map.requests.MapCreateRequest;
import net.hollowcube.mapmaker.map.requests.MapSearchParams;
import net.hollowcube.mapmaker.map.responses.PlayerTopTimesResponse;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.nio.channels.Channels;
import java.util.List;

@Blocking
public interface MapService {

    String LEADERBOARD_TOP_TIMES = "top_times";
    String LEADERBOARD_MAPS_BEATEN = "maps_beaten";

    MapData createMap(MapCreateRequest request);

    net.hollowcube.mapmaker.map.responses.MapSearchResponse searchMaps(MapSearchParams request);

    MapProgressBatchResponse getMapProgress(String playerId, List<String> mapIds);

    MapSearchResponse<MapData> searchOrgMaps(String authorizer, int page, int pageSize, String orgId);

    MapData getMap(String authorizer, String id);

    List<MapData> getMaps(String authorizer, List<String> mapIds);

    MapData getMapByPublishedId(String authorizer, long publishedId);

    void updateMap(String authorizer, String id, MapUpdateRequest update);

    void deleteMap(String authorizer, String id, @Nullable String reason);

    void beginVerification(String authorizer, String mapId);

    void deleteVerification(String authorizer, String mapId);

    MapData publishMap(String authorizer, String id);

    byte @Nullable [] getMapWorld(String id, boolean write);

    default @Nullable ReadableMapData getMapWorldAsStream(String id, boolean write) {
        var result = getMapWorld(id, write);
        return result == null ? null : new ReadableMapData(Channels.newChannel(new ByteArrayInputStream(result)), result.length);
    }

    void updateMapWorld(String id, byte[] worldData);

    void reportMap(String mapId, MapReportRequest req);

    LeaderboardData getGlobalLeaderboard(String name, @Nullable String playerId);

    LeaderboardData getPlaytimeLeaderboard(String mapId, @Nullable String playerId);

    void deletePlaytimeLeaderboard(String authorizer, String mapId, @Nullable String playerId);

    void restorePlaytimeLeaderboard(String authorizer, String mapId);

    // Save states
    SaveState createSaveState(String mapId, String playerId, int protocolVersion, @Nullable SaveStateType.Serializer<?> serializer);

    SaveState getLatestSaveState(String mapId, String playerId, @Nullable SaveStateType type, @Nullable SaveStateType.Serializer<?> serializer);

    @Nullable
    SaveState getBestSaveState(String mapId, String playerId);

    @Nullable
    SaveStateUpdateResponse updateSaveState(String mapId, String playerId, String id, SaveStateUpdateRequest update);

    void deleteSaveState(String mapId, String playerId, String id);

    MapRating getMapRating(String mapId, String playerId);

    void setMapRating(String mapId, String playerId, MapRating rating);

    MapPlayerData getMapPlayerData(String playerId);

    MapHistory getPlayerMapHistory(String playerId, int page, int amount);

    PlayerTopTimesResponse getPlayerTopTimes(String playerId, int page, int pageSize);

    class NotFoundError extends RuntimeException {
        public NotFoundError(String id) {
            super("Map not found: " + id);
        }
    }

    class SlotInUseError extends RuntimeException {
    }

    class NoPermissionError extends RuntimeException {
        public NoPermissionError() {
            super("No permission for map");
        }
    }

    class InternalError extends RuntimeException {
        public InternalError(String message) {
            super(message);
        }

        public InternalError(Throwable cause) {
            super(cause);
        }
    }

    class AlreadyExistsError extends RuntimeException {
        public AlreadyExistsError() {
            super("Already exists");
        }
    }
}
