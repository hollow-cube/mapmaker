package net.hollowcube.mapmaker.storage;

import net.hollowcube.mapmaker.error.Error;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.util.MongoUtil;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public interface MapStorage extends Storage {
    Error ERR_DUPLICATE_NAME = ERR_DUPLICATE_ENTRY.wrap("name {0}");

    static @NotNull MapStorage memory() {
        return new MapStorageMemory();
    }

    static MapStorage mongo(@NotNull String mongoUri) {
        return new MapStorageMongo(MongoUtil.getClient(mongoUri));
    }

    /**
     * Returns true the map data if created successfully, or throws if an error has occurred.
     *
     */
    @NotNull CompletableFuture<MapData> createMap(@NotNull MapData map);

    @NotNull CompletableFuture<MapData> getMapById(@NotNull String mapId);

    @NotNull CompletableFuture<Void> updateMap(@NotNull MapData map);

}
