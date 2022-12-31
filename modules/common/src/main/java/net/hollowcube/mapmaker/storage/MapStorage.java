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

    @NotNull FutureResult<String> lookupShortId(@NotNull String shortMapId);


    /**
     * Fetches the latest maps with the given offset and size. Used for paginating the map list right now, but will
     * be replaced with a more complicated "MapQuery" builder/system later.
     */
    @NotNull FutureResult<@NotNull List<MapData>> getLatestMaps(int offset, int size);


    // Other utilities

    @NotNull FutureResult<String> getNextId();

}
