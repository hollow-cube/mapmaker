package net.hollowcube.mapmaker.map;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.util.AbstractMemoryService;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class MapServiceMemory extends AbstractMemoryService implements MapService {
    private static final System.Logger logger = System.getLogger(MapServiceMemory.class.getName());

    private final Map<String, MapData> maps = new ConcurrentHashMap<>();
    private final Map<String, byte[]> worlds = new ConcurrentHashMap<>();

    @Override
    public @NotNull MapData createMap(@NotNull String authorizer, @NotNull String owner) {
        var id = UUID.randomUUID().toString();
        var map = new MapData(id, owner, new MapSettings(), 0, null);
        maps.put(id, map);
        return map;
    }

    @Override
    public @NotNull MapSearchResponse searchMaps(@NotNull String authorizer, int page, int pageSize, boolean building, boolean parkour, @NotNull String query) {
        if (SLOW) FutureUtil.sleep(ThreadLocalRandom.current().nextInt(2000));
        return new MapSearchResponse(
                page,
                maps.size() > (page + 1) * pageSize,
                maps.values().stream()
                        .filter(MapData::isPublished)
                        .sorted(Comparator.comparing(MapData::publishedAt).reversed())
                        .skip((long) page * pageSize)
                        .limit(pageSize)
                        //todo return real progress once that exists
                        .map(m -> new PersonalizedMapData(m, PersonalizedMapData.Progress.NONE))
                        .toList()
        );
    }

    @Override
    public @NotNull MapData getMap(@NotNull String authorizer, @NotNull String id) {
        var map = maps.get(id);
        if (map == null)
            throw new NotFoundError(id);
        return map;
    }

    @Override
    public void updateMap(@NotNull String authorizer, @NotNull String id, @NotNull MapUpdateRequest update) {
        var map = maps.get(id);
        if (map == null)
            throw new NotFoundError(id);

        var name = map.settings().getName();
        if (update.name != null) name = update.name;
        var icon = map.settings().getIcon();
        if (update.icon != null) icon = Material.fromNamespaceId(update.icon);
        var variant = map.settings().getVariant();
        if (update.variant != null) variant = update.variant;
        var spawnPoint = map.settings().getSpawnPoint();
        if (update.spawnPoint != null) spawnPoint = update.spawnPoint;

        var onlySprint = map.settings().isOnlySprint();
        if (update.onlySprint != null) onlySprint = update.onlySprint;
        var noSprint = map.settings().isNoSprint();
        if (update.noSprint != null) noSprint = update.noSprint;
        var noJump = map.settings().isNoJump();
        if (update.noJump != null) noJump = update.noJump;
        var noSneak = map.settings().isNoSneak();
        if (update.noSneak != null) noSneak = update.noSneak;

        var tags = map.settings().getTags();
        if (update.tags != null) tags = update.tags;

        var settings = new MapSettings(name, icon, variant, spawnPoint, onlySprint, noSprint, noJump, noSneak, tags);
        map = new MapData(map.id(), map.owner(), settings, map.publishedId(), map.publishedAt());
        maps.put(id, map);
    }

    @Override
    public void deleteMap(@NotNull String authorizer, @NotNull String id) {
        maps.remove(id);
    }

    @Override
    public void beginVerification(@NotNull String authorizer, @NotNull String mapId) {
        logger.log(System.Logger.Level.WARNING, "MapServiceMemory.beginVerification is a noop currently");
    }

    @Override
    public void deleteVerification(@NotNull String authorizer, @NotNull String mapId) {
        logger.log(System.Logger.Level.WARNING, "MapServiceMemory.deleteVerification is a noop currently");
    }

    @Override
    public @NotNull MapData publishMap(@NotNull String authorizer, @NotNull String id) {
        var map = maps.get(id);
        if (map == null)
            throw new NotFoundError(id);

        var publishedId = ThreadLocalRandom.current().nextInt(0, 1000000);
        var publishedAt = Instant.now();

        map = new MapData(map.id(), map.owner(), map.settings(), publishedId, publishedAt);
        maps.put(id, map);

        return map;
    }

    @Override
    public byte @Nullable [] getMapWorld(@NotNull String id, boolean write) {
        return worlds.get(id);
    }

    @Override
    public void updateMapWorld(@NotNull String id, byte @NotNull [] worldData) {
        worlds.put(id, worldData);
    }

    @Override
    public @NotNull LeaderboardData getPlaytimeLeaderboard(@NotNull String mapId, @Nullable String playerId) {
        logger.log(System.Logger.Level.WARNING, "MapServiceMemory.getPlaytimeLeaderboard is a noop currently");
        return new LeaderboardData(List.of(), null);
    }

    @Override
    public @NotNull SaveState createSaveState(@NotNull String mapId, @NotNull String playerId) {
        logger.log(System.Logger.Level.WARNING, "MapServiceMemory.getPlaytimeLeaderboard is a noop currently");
        var id = UUID.randomUUID().toString();
        return new SaveState(id, playerId, mapId);
    }

    @Override
    public @NotNull SaveState getSaveState(@NotNull String mapId, @NotNull String playerId, @NotNull String id) {
        logger.log(System.Logger.Level.WARNING, "MapServiceMemory.getSaveState is a noop currently");
        throw new NotFoundError(id);
    }

    @Override
    public @NotNull SaveState getLatestSaveState(@NotNull String mapId, @NotNull String playerId) {
        logger.log(System.Logger.Level.WARNING, "MapServiceMemory.getLatestSaveState is a noop currently");
        throw new NotFoundError("");
    }

    @Override
    public @NotNull SaveState getBestSaveState(@NotNull String mapId, @NotNull String playerId) {
        logger.log(System.Logger.Level.WARNING, "MapServiceMemory.getBestSaveState is a noop currently");
        throw new NotFoundError("");
    }

    @Override
    public void updateSaveState(@NotNull String mapId, @NotNull String playerId, @NotNull String id, @NotNull SaveStateUpdateRequest update) {
        logger.log(System.Logger.Level.WARNING, "MapServiceMemory.updateSaveState is a noop currently");
    }

    @Override
    public void deleteSaveState(@NotNull String mapId, @NotNull String playerId, @NotNull String id) {
        logger.log(System.Logger.Level.WARNING, "MapServiceMemory.deleteSaveState is a noop currently");
    }

    @Override
    public @Nullable InputStream getSaveStateReplay(@NotNull String mapId, @NotNull String playerId, @NotNull String saveStateId) {
        logger.log(System.Logger.Level.WARNING, "MapServiceMemory.getSaveStateReplay is a noop currently");
        return null;
    }

    @Override
    public void updateSaveStateReplay(@NotNull String mapId, @NotNull String playerId, @NotNull String saveStateId, @NotNull InputStream dataStream) {
        logger.log(System.Logger.Level.WARNING, "MapServiceMemory.updateSaveStateReplay is a noop currently");
    }
}
