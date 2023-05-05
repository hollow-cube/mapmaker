package net.hollowcube.mapmaker.storage;

import net.hollowcube.common.config.MongoConfig;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.model.MapQuery;
import net.hollowcube.mapmaker.storage.client.MongoClientFactory;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public @Blocking interface MapStorage {

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
     */
    @Blocking @NotNull MapData createMap(@NotNull MapData map);

    @Blocking @NotNull MapData getMapById(@NotNull String mapId);

    @Blocking void updateMap(@NotNull MapData map) throws NotFoundError;

    @Blocking @NotNull MapData deleteMap(@NotNull String mapId);

    @Blocking @NotNull String lookupShortId(@NotNull String shortMapId);


    /**
     * Fetches the latest maps with the given offset and size. Used for paginating the map list right now, but will
     * be replaced with a more complicated "MapQuery" builder/system later.
     */
    @Blocking @NotNull List<MapData> getLatestMaps(int offset, int size);

    @Blocking @NotNull List<MapData> queryMaps(@NotNull MapQuery query, int offset, int size);


    // Other utilities

    @Blocking @NotNull String getNextId();


    class NotFoundError extends RuntimeException { }

    class DuplicateEntryError extends RuntimeException { }

}
