package net.hollowcube.mapmaker.storage;

import net.hollowcube.common.config.MongoConfig;
import net.hollowcube.common.result.Error;
import net.hollowcube.common.result.FutureResult;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.storage.client.MongoClientFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface MapStorage {

    Error ERR_NOT_FOUND = Error.of("map not found");
    Error ERR_DUPLICATE_ENTRY = Error.of("map already exists");
    Error ERR_DUPLICATE_NAME = ERR_DUPLICATE_ENTRY.wrap("name {0}"); //todo remove me, names do not need to be unique.

    static @NotNull MapStorage memory() {
        return new MapStorageMemory();
    }

    static @NotNull FutureResult<MapStorage> mongo(@NotNull MongoConfig config) {
        var clientFactory = MongoClientFactory.get();
        return clientFactory.newClient(config)
                .map(client -> new MapStorageMongo(client, config))
                .flatMap(storage -> storage.init().map(unused -> storage));
    }

    /**
     * Returns true the map data if created successfully, or throws if an error has occurred.
     *
     */
    @NotNull FutureResult<MapData> createMap(@NotNull MapData map);

    @NotNull FutureResult<MapData> getMapById(@NotNull String mapId);

    @NotNull FutureResult<Void> updateMap(@NotNull MapData map);

    @NotNull FutureResult<MapData> deleteMap(@NotNull String mapId);


    // Player specific map searches

    @NotNull FutureResult<List<MapData>> getMapsByPlayer(@NotNull String playerId);

    @NotNull FutureResult<MapData> getPlayerMap(@NotNull String playerId, @NotNull String nameOrId);


    // Other utilities

    @NotNull FutureResult<String> getNextId();

}
