package net.hollowcube.mapmaker.storage;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import net.hollowcube.common.config.MongoConfig;
import net.hollowcube.common.result.Error;
import net.hollowcube.common.result.FutureResult;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.model.MapQuery;
import net.hollowcube.mapmaker.storage.client.MongoClientFactory;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface MapStorage {

    Error ERR_NOT_FOUND = Error.of("map not found");
    Error ERR_DUPLICATE_ENTRY = Error.of("map already exists");

    static @NotNull MapStorage memory() {
        return new MapStorageMemory();
    }

    @Blocking
    static @NotNull MapStorage mongo(@NotNull MongoConfig config) {
        var client = MongoClientFactory.get().newClient(config);
        return new MapStorageMongo(client, config);
    }

    /**
     * Returns true the map data if created successfully, or throws if an error has occurred.
     *
     */
    @NotNull FutureResult<MapData> createMap(@NotNull MapData map);

    @Blocking @NotNull MapData getMapById(@NotNull String mapId);

    @Blocking void updateMap(@NotNull MapData map) throws NotFoundError;

    @NotNull FutureResult<MapData> deleteMap(@NotNull String mapId);

    @NotNull FutureResult<String> lookupShortId(@NotNull String shortMapId);


    /**
     * Fetches the latest maps with the given offset and size. Used for paginating the map list right now, but will
     * be replaced with a more complicated "MapQuery" builder/system later.
     */
    @NotNull FutureResult<@NotNull List<MapData>> getLatestMaps(int offset, int size);

    @NotNull FutureResult<@NotNull List<MapData>> queryMaps(@NotNull MapQuery query, int offset, int size);


    // Other utilities

    @NotNull FutureResult<String> getNextId();


    class NotFoundError extends RuntimeException { }

}
