package net.hollowcube.mapmaker.storage;

import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.result.FutureResult;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

class MapStorageMemory implements MapStorage {
    private final Map<String, MapData> mapsById = new ConcurrentHashMap<>();

    @Override
    public @NotNull FutureResult<MapData> createMap(@NotNull MapData map) {
        var duplicateName = mapsById.values().stream()
                .filter(storedMap -> storedMap.getName().equalsIgnoreCase(map.getName()))
                .limit(1)
                .count() == 1;
        if (duplicateName)
            return FutureResult.error(ERR_DUPLICATE_NAME);
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
}
