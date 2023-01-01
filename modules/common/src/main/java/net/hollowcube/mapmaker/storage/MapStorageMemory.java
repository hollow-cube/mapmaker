package net.hollowcube.mapmaker.storage;

import net.hollowcube.common.result.FutureResult;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.model.MapQuery;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

class MapStorageMemory implements MapStorage {
    private final Map<String, MapData> mapsById = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    @Override
    public @NotNull FutureResult<MapData> createMap(@NotNull MapData map) {
        var existing = mapsById.putIfAbsent(map.getId(), map);
        if (existing != null)
            return FutureResult.error(ERR_DUPLICATE_ENTRY);
        return FutureResult.of(map);
    }

    @Override
    public @NotNull FutureResult<MapData> getMapById(@NotNull String mapId) {
        var map = mapsById.get(mapId);
        if (map == null)
            return FutureResult.error(ERR_NOT_FOUND);
        return FutureResult.of(map);
    }

    @Override
    public @NotNull FutureResult<Void> updateMap(@NotNull MapData map) {
        if (!mapsById.containsKey(map.getId()))
            return FutureResult.error(ERR_NOT_FOUND);
        mapsById.put(map.getId(), map);
        return FutureResult.of(null);
    }

    @Override
    public @NotNull FutureResult<MapData> deleteMap(@NotNull String mapId) {
        if (!mapsById.containsKey(mapId))
            return FutureResult.error(ERR_NOT_FOUND);
        return FutureResult.of(mapsById.remove(mapId));
    }

    @Override
    public @NotNull FutureResult<String> lookupShortId(@NotNull String shortMapId) {
        return mapsById.values().stream()
                .filter(map -> map.getPublishedId().equalsIgnoreCase(shortMapId))
                .findFirst()
                .map(MapData::getId)
                .map(FutureResult::of)
                .orElse(FutureResult.error(ERR_NOT_FOUND));
    }

    @Override
    public @NotNull FutureResult<@NotNull List<MapData>> getLatestMaps(int offset, int size) {
        return FutureResult.of(mapsById.values().stream()
                .filter(MapData::isPublished)
                .sorted(Comparator.comparing(MapData::getPublishedAt).reversed())
                .skip(offset)
                .limit(size)
                .toList());
    }

    @Override
    public @NotNull FutureResult<@NotNull List<MapData>> queryMaps(@NotNull MapQuery query, int offset, int size) {
        return FutureResult.of(mapsById.values().stream()
                .filter(map -> {
                    if (query.author() != null && !query.author().equals(map.getOwner()))
                        return false;
                    //noinspection RedundantIfStatement
                    if (query.publishedOnly() != null && !map.isPublished())
                        return false;
                    return true;
                })
                .sorted(Comparator.comparing(MapData::getPublishedAt).reversed())
                .skip(offset)
                .limit(size)
                .toList());
    }

    @Override
    public @NotNull FutureResult<String> getNextId() {
        var n = nextId.getAndIncrement();
        var id = "00000" + Integer.toString(n, 36);
        return FutureResult.of(id.substring(id.length() - 5));
    }
}
