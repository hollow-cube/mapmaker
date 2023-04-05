package net.hollowcube.mapmaker.storage;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import net.hollowcube.common.config.MongoConfig;
import net.hollowcube.common.result.Error;
import net.hollowcube.common.result.FutureResult;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.model.MapQuery;
import net.hollowcube.mapmaker.storage.client.MongoClientFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface MapStorage {

    class NotFoundError extends RuntimeException { }

    Error ERR_NOT_FOUND = Error.of("map not found");
    Error ERR_DUPLICATE_ENTRY = Error.of("map already exists");

    static @NotNull MapStorage memory() {
        return new MapStorageMemory();
    }

    static @NotNull ListenableFuture<@NotNull MapStorage> mongo(@NotNull MongoConfig config) {
        var clientFactory = MongoClientFactory.get();
        return Futures.transform(
                clientFactory.newClient(config),
                client -> new MapStorageMongo(client, config),
                Runnable::run
        );
    }

    /**
     * Returns true the map data if created successfully, or throws if an error has occurred.
     *
     */
    @NotNull FutureResult<MapData> createMap(@NotNull MapData map);

    @NotNull ListenableFuture<MapData> getMapById(@NotNull String mapId);

    @NotNull FutureResult<Void> updateMap(@NotNull MapData map);

    @NotNull FutureResult<MapData> deleteMap(@NotNull String mapId);

    @NotNull FutureResult<String> lookupPublishedId(@NotNull String publishedId);

    @NotNull FutureResult<String> lookupAliasId(@NotNull String aliasId);

    /**
     * Fetches the latest maps with the given offset and size. Used for paginating the map list right now, but will
     * be replaced with a more complicated "MapQuery" builder/system later.
     */
    @NotNull FutureResult<@NotNull List<MapData>> getLatestMaps(int offset, int size);

    @NotNull FutureResult<@NotNull List<MapData>> queryMaps(@NotNull MapQuery query, int offset, int size);


    // Other utilities

    @NotNull FutureResult<String> getNextId();

    /**
     * Given a unique map ID, spit out a string formatted ID "123-456-789" with padding zeros.
     * @param n id as integer
     * @return formatted string ID
     */
    @NotNull
    default String getFormattedId(Integer n) {
        if (n > Math.pow(10, 9)) {
            System.out.println("Oh no, there's more than 1 billion maps");
            return "YOU-ARE-DED";
        }
        //TODO should we create a hash function so the number isn't just incrementing?
        var id = new StringBuilder(String.format("%09d", n));
        var length = id.length();
        for (int i = length - 3; i > 0; i-= 3) {
            id.insert(i, '-');
        }
        return id.toString();
    }

}
