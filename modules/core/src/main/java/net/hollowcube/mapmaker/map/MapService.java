package net.hollowcube.mapmaker.map;

import net.hollowcube.mapmaker.util.Response;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.List;

@Blocking
public interface MapService {

    /**
     * Creates a new map in the map service with the given owner.
     *
     * @return The created map
     */
    @NotNull Response<MapData> createMap(@NotNull MapPlayerData player, int slot);

    @NotNull MapSearchResponse searchMaps(@NotNull String authorizer, int page, int pageSize, boolean building, boolean parkour, @NotNull String query);

    @NotNull MapSearchResponse searchMaps(@NotNull MapSearchRequest request);

    @NotNull MapData getMap(@NotNull String authorizer, @NotNull String id);

    @NotNull MapData getMapByPublishedId(@NotNull String authorizer, long publishedId);

    void updateMap(@NotNull String authorizer, @NotNull String id, @NotNull MapUpdateRequest update);

    void deleteMap(@NotNull MapPlayerData player, @NotNull String id);

    void beginVerification(@NotNull String authorizer, @NotNull String mapId);

    void deleteVerification(@NotNull String authorizer, @NotNull String mapId);

    @NotNull MapData publishMap(@NotNull String authorizer, @NotNull String id);

    byte @Nullable [] getMapWorld(@NotNull String id, boolean write);

    void updateMapWorld(@NotNull String id, byte @NotNull [] worldData);

    @NotNull LeaderboardData getPlaytimeLeaderboard(@NotNull String mapId, @Nullable String playerId);

    // Save states
    @NotNull SaveState createSaveState(@NotNull String mapId, @NotNull String playerId);

    @NotNull SaveState getSaveState(@NotNull String mapId, @NotNull String playerId, @NotNull String id);

    default @NotNull SaveState getLatestSaveState(@NotNull String mapId, @NotNull String playerId) {
        return getLatestSaveState(mapId, playerId, null);
    }

    @NotNull SaveState getLatestSaveState(@NotNull String mapId, @NotNull String playerId, @Nullable SaveStateType type);

    @Nullable SaveState getBestSaveState(@NotNull String mapId, @NotNull String playerId);

    void updateSaveState(@NotNull String mapId, @NotNull String playerId, @NotNull String id, @NotNull SaveStateUpdateRequest update);

    void deleteSaveState(@NotNull String mapId, @NotNull String playerId, @NotNull String id);

    @Nullable InputStream getSaveStateReplay(@NotNull String mapId, @NotNull String playerId, @NotNull String saveStateId);

    void updateSaveStateReplay(@NotNull String mapId, @NotNull String playerId, @NotNull String saveStateId, @NotNull InputStream dataStream);

    @NotNull MapPlayerData getMapPlayerData(@NotNull String playerId);

    // Legacy

    @NotNull List<LegacyMapInfo> getLegacyMaps(@NotNull String authorizer, @NotNull String playerId);

    @NotNull MapData.WithSlot importLegacyMap(@NotNull String authorizer, @NotNull String playerId, @NotNull String legacyMapId);

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
