package net.hollowcube.mapmaker.storage;

import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.result.Error;
import net.hollowcube.mapmaker.result.FutureResult;
import net.hollowcube.mapmaker.util.MongoUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

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
    @NotNull FutureResult<MapData> createMap(@NotNull MapData map);

    @NotNull FutureResult<MapData> getMapById(@NotNull String mapId);

    @NotNull FutureResult<Void> updateMap(@NotNull MapData map);


    // Player specific map searches

    @NotNull FutureResult<List<MapData>> getMapsByPlayer(@NotNull String playerId);

    @NotNull FutureResult<MapData> getPlayerMap(@NotNull String playerId, @NotNull String nameOrId);

}
