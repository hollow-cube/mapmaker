package net.hollowcube.mapmaker.storage;

import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.util.MongoUtil;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public interface MapStorage extends Storage {

    static @NotNull MapStorage memory() {
        return new MapStorageMemory();
    }

    static MapStorage mongo(@NotNull String mongoUri) {
        return new MapStorageMongo(MongoUtil.getClient(mongoUri));
    }

    @NotNull CompletableFuture<MapData> createMap(@NotNull MapData map);

    @NotNull CompletableFuture<MapData> getMapById(@NotNull String mapId);

}
