package net.hollowcube.mapmaker.storage;

import net.hollowcube.mapmaker.model.MapData;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

class MapStorageMemory implements MapStorage {
    private final Map<String, MapData> mapsById = new ConcurrentHashMap<>();

    @Override
    public @NotNull CompletableFuture<MapData> createMap(@NotNull MapData map) {
        var existing = mapsById.putIfAbsent(map.getId(), map);
        if (existing != null)
            return CompletableFuture.failedFuture(DUPLICATE_ENTRY);
        return CompletableFuture.completedFuture(map);
    }

    @Override
    public @NotNull CompletableFuture<MapData> getMapById(@NotNull String mapId) {
        var map = mapsById.get(mapId);
        if (map == null)
            return CompletableFuture.failedFuture(NOT_FOUND);
        return CompletableFuture.completedFuture(map);
    }

    @Override
    public @NotNull CompletableFuture<Void> updateMap(@NotNull MapData map) {
        if (!mapsById.containsKey(map.getId()))
            return CompletableFuture.failedFuture(NOT_FOUND);
        mapsById.put(map.getId(), map);
        return CompletableFuture.completedFuture(null);
    }
}
