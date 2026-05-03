package net.hollowcube.mapmaker.misc.noop;

import net.hollowcube.mapmaker.map.*;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
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
    public byte @Nullable [] getMapWorld(@NotNull String id, boolean write) {
        return null;
    }

    @Override
    public void updateMapWorld(@NotNull String id, byte @NotNull [] worldData, long loadTime) {
        // Do nothing we arent going to save the world
    }

    @Override
    public @NotNull LeaderboardData getGlobalLeaderboard(@NotNull String name, @Nullable String playerId) {
        return new LeaderboardData(List.of(), new LeaderboardData.Entry(UUID.randomUUID().toString(), 0, 1000));
    }

    @Override
    public @NotNull LeaderboardData getPlaytimeLeaderboard(@NotNull String mapId, @Nullable String playerId) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void deletePlaytimeLeaderboard(@NotNull String authorizer, @NotNull String mapId, @Nullable String playerId, boolean notify) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void restorePlaytimeLeaderboard(@NotNull String authorizer, @NotNull String mapId) {
        throw new UnsupportedOperationException("not implemented");
    }

}
