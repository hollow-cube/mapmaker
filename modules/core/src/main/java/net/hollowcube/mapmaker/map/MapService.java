package net.hollowcube.mapmaker.map;

import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Blocking
public interface MapService {
    @NotNull ExecutorService VIRTUAL_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    @NotNull String LEADERBOARD_TOP_TIMES = "top_times";
    @NotNull String LEADERBOARD_MAPS_BEATEN = "maps_beaten";

    /**
     * Creates a new map in the map service with the given owner.
     *
     * @return The created map
     */
    @NotNull MapData createMap(@NotNull MapPlayerData player, int slot);

    @NotNull MapData createOrgMap(@NotNull String authorizer, @NotNull String orgId);

    @NotNull MapSearchResponse<PersonalizedMapData> searchMaps(@NotNull String authorizer, @NotNull String sort, int page, int pageSize, boolean building, boolean parkour, @NotNull String query);

    @NotNull MapSearchResponse<PersonalizedMapData> searchMaps(@NotNull MapSearchRequest request);

    @NotNull MapSearchResponse<MapData> searchOrgMaps(@NotNull String authorizer, int page, int pageSize, @NotNull String orgId);

    @NotNull MapData getMap(@NotNull String authorizer, @NotNull String id);

    @NotNull MapData getMapByPublishedId(@NotNull String authorizer, long publishedId);

    void updateMap(@NotNull String authorizer, @NotNull String id, @NotNull MapUpdateRequest update);

    void deleteMap(@NotNull String authorizer, @NotNull String id, @Nullable String reason);

    void beginVerification(@NotNull String authorizer, @NotNull String mapId);

    void deleteVerification(@NotNull String authorizer, @NotNull String mapId);

    @NotNull MapData publishMap(@NotNull String authorizer, @NotNull String id);

    byte @Nullable [] getMapWorld(@NotNull String id, boolean write);

    void updateMapWorld(@NotNull String id, byte @NotNull [] worldData);

    void reportMap(@NotNull String mapId, @NotNull MapReportRequest req);

    @NotNull LeaderboardData getGlobalLeaderboard(@NotNull String name, @Nullable String playerId);

    @NotNull LeaderboardData getPlaytimeLeaderboard(@NotNull String mapId, @Nullable String playerId);

    void deletePlaytimeLeaderboard(@NotNull String authorizer, @NotNull String mapId, @Nullable String playerId);

    void restorePlaytimeLeaderboard(@NotNull String authorizer, @NotNull String mapId);

    // Save states
    @NotNull SaveState createSaveState(@NotNull String mapId, @NotNull String playerId);

    default @NotNull SaveState getLatestSaveState(@NotNull String mapId, @NotNull String playerId) {
        return getLatestSaveState(mapId, playerId, null);
    }

    @NotNull SaveState getLatestSaveState(@NotNull String mapId, @NotNull String playerId, @Nullable SaveStateType type);

    @Nullable SaveState getBestSaveState(@NotNull String mapId, @NotNull String playerId);

    void updateSaveState(@NotNull String mapId, @NotNull String playerId, @NotNull String id, @NotNull SaveStateUpdateRequest update);

    void deleteSaveState(@NotNull String mapId, @NotNull String playerId, @NotNull String id);

    @Nullable InputStream getSaveStateReplay(@NotNull String mapId, @NotNull String playerId, @NotNull String saveStateId);

    void updateSaveStateReplay(@NotNull String mapId, @NotNull String playerId, @NotNull String saveStateId, @NotNull InputStream dataStream);

    @NotNull MapRating getMapRating(@NotNull String mapId, @NotNull String playerId);

    void setMapRating(@NotNull String mapId, @NotNull String playerId, @NotNull MapRating rating);

    @NotNull MapPlayerData getMapPlayerData(@NotNull String playerId);

    // Legacy

    @NotNull List<LegacyMapInfo> getLegacyMaps(@NotNull String authorizer, @NotNull String playerId);

    @NotNull MapData.WithSlot importLegacyMap(@NotNull String authorizer, @NotNull String playerId, @NotNull String legacyMapId);

    // Ignore this stuff :|

    void uploadPerfdump(@NotNull String name, @NotNull Path data);

    class NotFoundError extends RuntimeException {
        public NotFoundError(@NotNull String id) {
            super("Map not found: " + id);
        }
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
}
