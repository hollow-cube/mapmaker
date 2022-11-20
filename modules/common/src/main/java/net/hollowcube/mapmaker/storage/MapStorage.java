package net.hollowcube.mapmaker.storage;

import net.hollowcube.mapmaker.model.MapData;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public interface MapStorage extends Storage {

    static @NotNull MapStorage memory() {
        return new MapStorageMemory();
    }

    @NotNull CompletableFuture<MapData> createMap(@NotNull MapData map);

    @NotNull CompletableFuture<MapData> getMapById(@NotNull String mapId);

}
