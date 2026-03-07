package net.hollowcube.mapmaker.map;

import net.hollowcube.mapmaker.map.requests.MapCreateRequest;
import net.hollowcube.mapmaker.map.requests.MapSearchParams;
import net.hollowcube.mapmaker.map.responses.PlayerTopTimesResponse;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Blocking
public interface MapService {
    @NotNull
    ExecutorService VIRTUAL_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    @NotNull
    String LEADERBOARD_TOP_TIMES = "top_times";
    @NotNull
    String LEADERBOARD_MAPS_BEATEN = "maps_beaten";


    @NotNull
    MapData createMap(@NotNull MapCreateRequest request);

    @NotNull
    net.hollowcube.mapmaker.map.responses.MapSearchResponse searchMaps(@NotNull MapSearchParams request);

    @NotNull
    MapProgressBatchResponse getMapProgress(@NotNull String playerId, @NotNull List<String> mapIds);

    @NotNull
    MapSearchResponse<MapData> searchOrgMaps(@NotNull String authorizer, int page, int pageSize, @NotNull String orgId);

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

    void updateMapWorld(@NotNull String id, byte @NotNull [] worldData);

    void reportMap(@NotNull String mapId, @NotNull MapReportRequest req);

    @NotNull
    List<MapBuilder> getMapBuilders(@NotNull String mapId);

    @NotNull
    List<MapData> getMapsPlayerIsBuilderOn(@NotNull String playerId);

    void inviteMapBuilder(@NotNull String mapId, @NotNull String playerId);

    void acceptMapBuilderRequest(@NotNull String mapId, @NotNull String playerId);

    void rejectMapBuilderRequest(@NotNull String mapId, @NotNull String playerId);

    void removeMapBuilder(@NotNull String mapId, @NotNull String playerId);

    @NotNull
    LeaderboardData getGlobalLeaderboard(@NotNull String name, @Nullable String playerId);

    @NotNull
    LeaderboardData getPlaytimeLeaderboard(@NotNull String mapId, @Nullable String playerId);

    void deletePlaytimeLeaderboard(@NotNull String authorizer, @NotNull String mapId, @Nullable String playerId);

    void restorePlaytimeLeaderboard(@NotNull String authorizer, @NotNull String mapId);

    // Save states
    @NotNull
    SaveState createSaveState(@NotNull String mapId, @NotNull String playerId, int protocolVersion, @Nullable SaveStateType.Serializer<?> serializer);

    @NotNull
    SaveState getLatestSaveState(@NotNull String mapId, @NotNull String playerId, @Nullable SaveStateType type, @Nullable SaveStateType.Serializer<?> serializer);

    @Nullable
    SaveState getBestSaveState(@NotNull String mapId, @NotNull String playerId);

    @Nullable
    SaveStateUpdateResponse updateSaveState(@NotNull String mapId, @NotNull String playerId, @NotNull String id, @NotNull SaveStateUpdateRequest update);

    void deleteSaveState(@NotNull String mapId, @NotNull String playerId, @NotNull String id);

    @Nullable
    InputStream getSaveStateReplay(@NotNull String mapId, @NotNull String playerId, @NotNull String saveStateId);

    void updateSaveStateReplay(@NotNull String mapId, @NotNull String playerId, @NotNull String saveStateId, @NotNull InputStream dataStream);

    @NotNull MapRating getMapRating(@NotNull String mapId, @NotNull String playerId);

    void setMapRating(@NotNull String mapId, @NotNull String playerId, @NotNull MapRating rating);

    @NotNull
    MapPlayerData getMapPlayerData(@NotNull String playerId);

    @NotNull
    List<MapData> getPlayerMapSlots(@NotNull String playerId);

    @NotNull
    MapHistory getPlayerMapHistory(@NotNull String playerId, int page, int amount);

    @NotNull
    PlayerTopTimesResponse getPlayerTopTimes(@NotNull String playerId, int page, int pageSize);

    class NotFoundError extends RuntimeException {
        public NotFoundError(@NotNull String id) {
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
        public InternalError(@NotNull String message) {
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

    class MapBuilderNoSlotsError extends RuntimeException {
        public MapBuilderNoSlotsError() {
            super("No slots remaining");
        }
    }
}
