package net.hollowcube.mapmaker.storage;

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
    public @NotNull MapData createMap(@NotNull MapData map) {
        var existing = mapsById.putIfAbsent(map.getId(), map);
        if (existing != null)
            throw new DuplicateEntryError();
        return map;
    }

    @Override
    public @NotNull MapData getMapById(@NotNull String mapId) {
        var map = mapsById.get(mapId);
        if (map == null)
            throw new NotFoundError();
        return map;
    }

    @Override
    public void updateMap(@NotNull MapData map) {
        if (!mapsById.containsKey(map.getId()))
            throw new NotFoundError();
        mapsById.put(map.getId(), map);
    }

    @Override
    public @NotNull MapData deleteMap(@NotNull String mapId) {
        if (!mapsById.containsKey(mapId))
            throw new NotFoundError();
        return mapsById.remove(mapId);
    }

    @Override
    public @NotNull String lookupShortId(@NotNull String shortMapId) {
        return mapsById.values().stream()
                .filter(map -> map.getPublishedId().equalsIgnoreCase(shortMapId))
                .findFirst()
                .map(MapData::getId)
                .orElseThrow(NotFoundError::new);
    }

    @Override
    public @NotNull List<MapData> getLatestMaps(int offset, int size) {
        return mapsById.values().stream()
                .filter(MapData::isPublished)
                .sorted(Comparator.comparing(MapData::getPublishedAt).reversed())
                .skip(offset)
                .limit(size)
                .toList();
    }

    @Override
    public @NotNull List<MapData> queryMaps(@NotNull MapQuery query, int offset, int size) {
        return mapsById.values().stream()
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
                .toList();
    }

    @Override
    public @NotNull String getNextId() {
        var n = nextId.getAndIncrement();
        var id = "00000" + Integer.toString(n, 36);
        return id.substring(id.length() - 5);
    }
}
