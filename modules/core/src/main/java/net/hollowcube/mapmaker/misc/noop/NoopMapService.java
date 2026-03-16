package net.hollowcube.mapmaker.misc.noop;

import com.google.gson.JsonObject;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.map.requests.MapCreateRequest;
import net.hollowcube.mapmaker.map.requests.MapSearchParams;
import net.hollowcube.mapmaker.map.responses.PlayerTopTimesResponse;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NoopMapService implements MapService {
    private final Map<String, MapData> staticMaps = Map.of(
        "62da0aaf-8cad-4c13-869c-02b07688988d", new MapData("62da0aaf-8cad-4c13-869c-02b07688988d", UUID.randomUUID().toString(), new MapSettings(
            "Test Map", Material.ACACIA_BOAT, MapSize.NORMAL, MapVariant.PARKOUR, new Pos(0.5, 40, 0.5), List.of(MapTags.Tag.STRUCTURE)
        ), 0, null),
        "3b9540fb-9100-484d-8bc6-5c3d61eff3a1", new MapData("3b9540fb-9100-484d-8bc6-5c3d61eff3a1", "597481a0-02fb-441c-9188-c407bec05084", new MapSettings(
            "Published 1", Material.DIAMOND, MapSize.NORMAL, MapVariant.PARKOUR, Pos.ZERO, List.of(MapTags.Tag.STRUCTURE)
        ), 1, Instant.now()),
        "fd5771c0-c545-4d30-94fc-47e574e0fb64", new MapData("fd5771c0-c545-4d30-94fc-47e574e0fb64", "597481a0-02fb-441c-9188-c407bec05084", new MapSettings(
            "Published 2", Material.STICK, MapSize.NORMAL, MapVariant.PARKOUR, Pos.ZERO, List.of(MapTags.Tag.STRUCTURE)
        ), 2, Instant.now()),
        "5b1e433c-7b98-4ff1-bab1-053e83eab939", new MapData("5b1e433c-7b98-4ff1-bab1-053e83eab939", "597481a0-02fb-441c-9188-c407bec05084", new MapSettings(
            "Published 3", Material.MAGENTA_DYE, MapSize.NORMAL, MapVariant.PARKOUR, Pos.ZERO, List.of(MapTags.Tag.STRUCTURE)
        ), 3, Instant.now())
    );

    @Override
    public MapData createMap(MapCreateRequest request) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public net.hollowcube.mapmaker.map.responses.MapSearchResponse searchMaps(MapSearchParams request) {
        return new net.hollowcube.mapmaker.map.responses.MapSearchResponse(
            0, 1,
            staticMaps.values().stream().filter(m -> m instanceof PersonalizedMapData && m.publishedAt() != null).toList()
        );
    }

    @Override
    public MapProgressBatchResponse getMapProgress(String playerId, List<String> mapIds) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public MapSearchResponse<MapData> searchOrgMaps(String authorizer, int page, int pageSize, String orgId) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public MapData getMap(String authorizer, String id) {
        var map = staticMaps.get(id);
        if (map == null) {
            throw new NotFoundError(id);
        }
        return map;
    }

    @Override
    public List<MapData> getMaps(String authorizer, List<String> mapIds) {
        return mapIds.stream().map(staticMaps::get).toList();
    }

    @Override
    public MapData getMapByPublishedId(String authorizer, long publishedId) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void updateMap(String authorizer, String id, MapUpdateRequest update) {
        // Just dont do anything
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
        return null;
    }

    @Override
    public void updateMapWorld(String id, byte[] worldData) {
        // Do nothing we arent going to save the world
    }

    @Override
    public void reportMap(String mapId, MapReportRequest req) {

    }

    @Override
    public LeaderboardData getGlobalLeaderboard(String name, @Nullable String playerId) {
        return new LeaderboardData(List.of(), new LeaderboardData.Entry(UUID.randomUUID().toString(), 0, 1000));
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
    public SaveState createSaveState(String mapId, String playerId, int protocolVersion, @Nullable SaveStateType.Serializer<?> serializer) {
        var obj = serializer.codec().decode(Transcoder.JSON, new JsonObject()).orElseThrow();
        return new SaveState(UUID.randomUUID().toString(), playerId, mapId, SaveStateType.PLAYING, serializer, obj);
    }

    @Override
    public SaveState getLatestSaveState(String mapId, String playerId, @Nullable SaveStateType type, @Nullable SaveStateType.Serializer<?> serializer) {
        throw new NotFoundError(mapId);
    }

    @Override
    public @Nullable SaveState getBestSaveState(String mapId, String playerId) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public @Nullable SaveStateUpdateResponse updateSaveState(String mapId, String playerId, String id, SaveStateUpdateRequest update) {
        return null;
    }

    @Override
    public void deleteSaveState(String mapId, String playerId, String id) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public MapRating getMapRating(String mapId, String playerId) {
        return new MapRating(MapRating.State.UNRATED, null);
    }

    @Override
    public void setMapRating(String mapId, String playerId, MapRating rating) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public MapPlayerData getMapPlayerData(String playerId) {
        return new MapPlayerData(
            playerId,
            new String[]{null, "62da0aaf-8cad-4c13-869c-02b07688988d", null, null},
            null, null, null
        );
    }

    @Override
    public MapHistory getPlayerMapHistory(String playerId, int page, int amount) {
        return new MapHistory(page, false, List.of(new MapHistory.Entry("62da0aaf-8cad-4c13-869c-02b07688988d")));
    }

    @Override
    public PlayerTopTimesResponse getPlayerTopTimes(String playerId, int page, int pageSize) {
        throw new UnsupportedOperationException("not implemented");
    }

}
