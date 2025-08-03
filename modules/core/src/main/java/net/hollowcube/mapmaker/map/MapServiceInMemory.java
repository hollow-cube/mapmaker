package net.hollowcube.mapmaker.map;

import net.hollowcube.mapmaker.map.requests.MapCreateRequest;
import net.hollowcube.mapmaker.map.requests.MapSearchParams;
import net.hollowcube.mapmaker.map.responses.MapSearchResponse;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/// A (mostly) functional map service with all data stored in memory.
///
/// Unsupported operations will throw "not implemented", but should be implemented if needed.
@NotNullByDefault
public class MapServiceInMemory implements MapService {
    private final Map<String, MapData> maps = new ConcurrentHashMap<>();
    private final Map<String, byte[]> mapWorlds = new ConcurrentHashMap<>();
    private final Map<String, SaveState> saveStates = new ConcurrentHashMap<>();

    @Override
    public MapData createMap(MapCreateRequest request) {
        throw new UnsupportedOperationException("not implemented");
    }

    public void addMap(MapData map) {
        maps.put(map.id(), map);
    }

    @Override
    public MapSearchResponse searchMaps(MapSearchParams request) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public MapProgressBatchResponse getMapProgress(String playerId, List<String> mapIds) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public net.hollowcube.mapmaker.map.MapSearchResponse<MapData> searchOrgMaps(String authorizer, int page, int pageSize, String orgId) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public MapData getMap(String authorizer, String id) {
        var map = maps.get(id);
        if (map == null) throw new MapService.NotFoundError(id);
        return map;
    }

    @Override
    public List<MapData> getMaps(String authorizer, List<String> mapIds) {
        var result = new ArrayList<MapData>();
        for (var id : mapIds) {
            var map = maps.get(id);
            if (map != null) result.add(map);
        }
        return result;
    }

    @Override
    public MapData getMapByPublishedId(String authorizer, long publishedId) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void updateMap(String authorizer, String id, MapUpdateRequest update) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void deleteMap(String authorizer, String id, @Nullable String reason) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void beginVerification(String authorizer, String mapId) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void deleteVerification(String authorizer, String mapId) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public MapData publishMap(String authorizer, String id) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public byte @Nullable [] getMapWorld(String id, boolean write) {
        return mapWorlds.get(id);
    }

    @Override
    public void updateMapWorld(String id, byte[] worldData) {
        mapWorlds.put(id, worldData);
    }

    @Override
    public void reportMap(String mapId, MapReportRequest req) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public LeaderboardData getGlobalLeaderboard(String name, @Nullable String playerId) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public LeaderboardData getPlaytimeLeaderboard(String mapId, @Nullable String playerId) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void deletePlaytimeLeaderboard(String authorizer, String mapId, @Nullable String playerId) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void restorePlaytimeLeaderboard(String authorizer, String mapId) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public SaveState createSaveState(String mapId, String playerId, int protocolVersion, SaveStateType.@Nullable Serializer<?> serializer) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public SaveState getLatestSaveState(String mapId, String playerId, @Nullable SaveStateType type, SaveStateType.@Nullable Serializer<?> serializer) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public @Nullable SaveState getBestSaveState(String mapId, String playerId) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public @Nullable SaveStateUpdateResponse updateSaveState(String mapId, String playerId, String id, SaveStateUpdateRequest update) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void deleteSaveState(String mapId, String playerId, String id) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public @Nullable InputStream getSaveStateReplay(String mapId, String playerId, String saveStateId) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void updateSaveStateReplay(String mapId, String playerId, String saveStateId, InputStream dataStream) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public MapRating getMapRating(String mapId, String playerId) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void setMapRating(String mapId, String playerId, MapRating rating) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public MapPlayerData getMapPlayerData(String playerId) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public MapHistory getPlayerMapHistory(String playerId, int page, int amount) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public List<LegacyMapInfo> getLegacyMaps(String authorizer, String playerId) {
        return List.of();
    }

    @Override
    public MapData.WithSlot importLegacyMap(String authorizer, String playerId, String legacyMapId) {
        throw new MapService.NotFoundError(legacyMapId);
    }
}
