package net.hollowcube.mapmaker.misc.noop;

import net.hollowcube.mapmaker.map.*;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NoopMapService implements MapService {
    private final Map<String, MapData> staticMaps = Map.of(
            "62da0aaf-8cad-4c13-869c-02b07688988d", new MapData("62da0aaf-8cad-4c13-869c-02b07688988d", UUID.randomUUID().toString(), new MapSettings(
                    "Test Map", Material.ACACIA_BOAT, MapSize.NORMAL, MapVariant.PARKOUR, new Pos(0.5, 40, 0.5), true, false, false, false, List.of(MapTags.Tag.STRUCTURE)
            ), 0, null),
            "3b9540fb-9100-484d-8bc6-5c3d61eff3a1", new PersonalizedMapData(new MapData("3b9540fb-9100-484d-8bc6-5c3d61eff3a1", "597481a0-02fb-441c-9188-c407bec05084", new MapSettings(
                    "Published 1", Material.DIAMOND, MapSize.NORMAL, MapVariant.PARKOUR, Pos.ZERO, true, false, false, false, List.of(MapTags.Tag.STRUCTURE)
            ), 1, Instant.now()), PersonalizedMapData.Progress.COMPLETE),
            "fd5771c0-c545-4d30-94fc-47e574e0fb64", new PersonalizedMapData(new MapData("fd5771c0-c545-4d30-94fc-47e574e0fb64", "597481a0-02fb-441c-9188-c407bec05084", new MapSettings(
                    "Published 2", Material.STICK, MapSize.NORMAL, MapVariant.PARKOUR, Pos.ZERO, true, false, false, false, List.of(MapTags.Tag.STRUCTURE)
            ), 2, Instant.now()), PersonalizedMapData.Progress.STARTED),
            "5b1e433c-7b98-4ff1-bab1-053e83eab939", new PersonalizedMapData(new MapData("5b1e433c-7b98-4ff1-bab1-053e83eab939", "597481a0-02fb-441c-9188-c407bec05084", new MapSettings(
                    "Published 3", Material.MAGENTA_DYE, MapSize.NORMAL, MapVariant.PARKOUR, Pos.ZERO, true, false, false, false, List.of(MapTags.Tag.STRUCTURE)
            ), 3, Instant.now()), PersonalizedMapData.Progress.NONE)
    );

    @Override
    public @NotNull MapData createMap(@NotNull MapPlayerData player, int slot) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public @NotNull MapData createOrgMap(@NotNull String authorizer, @NotNull String orgId) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public @NotNull MapSearchResponse<PersonalizedMapData> searchMaps(@NotNull String authorizer, @NotNull String sort, int page, int pageSize, boolean building, boolean parkour, @NotNull String query) {
        return new MapSearchResponse<>(1, false, staticMaps.values().stream()
                .filter(m -> m instanceof PersonalizedMapData && m.publishedAt() != null)
                .map(m -> (PersonalizedMapData) m)
                .toList());
    }

    @Override
    public @NotNull MapSearchResponse<PersonalizedMapData> searchMaps(@NotNull MapSearchRequest request) {
        return new MapSearchResponse<>(1, false, staticMaps.values().stream()
                .filter(m -> m instanceof PersonalizedMapData && m.publishedAt() != null)
                .map(m -> (PersonalizedMapData) m)
                .toList());
    }

    @Override
    public @NotNull MapSearchResponse<MapData> searchMapsV2(@NotNull String authorizer, @NotNull String sort, int page, int pageSize, boolean building, boolean parkour, @NotNull String query) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public @NotNull MapProgressBatchResponse getMapProgress(@NotNull String playerId, @NotNull List<String> mapIds) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public @NotNull MapSearchResponse<MapData> searchOrgMaps(@NotNull String authorizer, int page, int pageSize, @NotNull String orgId) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public @NotNull MapData getMap(@NotNull String authorizer, @NotNull String id) {
        var map = staticMaps.get(id);
        if (map == null) {
            throw new NotFoundError(id);
        }
        return map;
    }

    @Override
    public @NotNull MapData getMapByPublishedId(@NotNull String authorizer, long publishedId) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void updateMap(@NotNull String authorizer, @NotNull String id, @NotNull MapUpdateRequest update) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void deleteMap(@NotNull String authorizer, @NotNull String id, @Nullable String reason) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void beginVerification(@NotNull String authorizer, @NotNull String mapId) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void deleteVerification(@NotNull String authorizer, @NotNull String mapId) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public @NotNull MapData publishMap(@NotNull String authorizer, @NotNull String id) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public byte @Nullable [] getMapWorld(@NotNull String id, boolean write) {
        return null;
    }

    @Override
    public void updateMapWorld(@NotNull String id, byte @NotNull [] worldData) {
        // Do nothing we arent going to save the world
    }

    @Override
    public void reportMap(@NotNull String mapId, @NotNull MapReportRequest req) {

    }

    @Override
    public @NotNull LeaderboardData getGlobalLeaderboard(@NotNull String name, @Nullable String playerId) {
        return null;
    }

    @Override
    public @NotNull LeaderboardData getPlaytimeLeaderboard(@NotNull String mapId, @Nullable String playerId) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void deletePlaytimeLeaderboard(@NotNull String authorizer, @NotNull String mapId, @Nullable String playerId) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void restorePlaytimeLeaderboard(@NotNull String authorizer, @NotNull String mapId) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public @NotNull SaveState createSaveState(@NotNull String mapId, @NotNull String playerId) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public @NotNull SaveState getLatestSaveState(@NotNull String mapId, @NotNull String playerId, @Nullable SaveStateType type) {
        return new SaveState(UUID.randomUUID().toString(), playerId, mapId, type == null ? SaveStateType.EDITING : type);
    }

    @Override
    public @Nullable SaveState getBestSaveState(@NotNull String mapId, @NotNull String playerId) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public @Nullable SaveStateUpdateResponse updateSaveState(@NotNull String mapId, @NotNull String playerId, @NotNull String id, @NotNull SaveStateUpdateRequest update) {
        return null;
    }

    @Override
    public void deleteSaveState(@NotNull String mapId, @NotNull String playerId, @NotNull String id) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public @Nullable InputStream getSaveStateReplay(@NotNull String mapId, @NotNull String playerId, @NotNull String saveStateId) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void updateSaveStateReplay(@NotNull String mapId, @NotNull String playerId, @NotNull String saveStateId, @NotNull InputStream dataStream) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public @NotNull MapRating getMapRating(@NotNull String mapId, @NotNull String playerId) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void setMapRating(@NotNull String mapId, @NotNull String playerId, @NotNull MapRating rating) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public @NotNull MapPlayerData getMapPlayerData(@NotNull String playerId) {
        return new MapPlayerData(
                playerId,
                4, new String[]{null, "62da0aaf-8cad-4c13-869c-02b07688988d", null, null},
                null, null
        );
    }

    @Override
    public @NotNull List<LegacyMapInfo> getLegacyMaps(@NotNull String authorizer, @NotNull String playerId) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public @NotNull MapData.WithSlot importLegacyMap(@NotNull String authorizer, @NotNull String playerId, @NotNull String legacyMapId) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void uploadPerfdump(@NotNull String name, @NotNull Path data) {
        throw new UnsupportedOperationException("not implemented");
    }
}
