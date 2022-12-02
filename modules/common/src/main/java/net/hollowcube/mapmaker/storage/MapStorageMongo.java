package net.hollowcube.mapmaker.storage;

import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoWriteConcernException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.InsertOneOptions;
import net.hollowcube.mapmaker.model.MapData;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

import static com.mongodb.client.model.Filters.eq;

public class MapStorageMongo implements MapStorage {
    private static final String DB_NAME = System.getProperty("mongo.db", "mapmaker");

    private final MongoClient client;

    public MapStorageMongo(@NotNull MongoClient client) {
        this.client = client;
    }

    @Override
    public @NotNull CompletableFuture<MapData> createMap(@NotNull MapData map) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                collection().insertOne(map);
            } catch (DuplicateKeyException ignored) {
                throw DUPLICATE_ENTRY;
            }
            return map;
        }, ForkJoinPool.commonPool());
    }

    @Override
    public @NotNull CompletableFuture<MapData> getMapById(@NotNull String mapId) {
        return CompletableFuture.supplyAsync(() -> {
            var filter = eq("_id", mapId);
            var result = collection().find(filter).limit(1).first();
            if (result == null)
                throw NOT_FOUND;
            return result;
        }, ForkJoinPool.commonPool());
    }

    @Override
    public @NotNull CompletableFuture<Void> updateMap(@NotNull MapData map) {
        return CompletableFuture.supplyAsync(() -> {
            var filter = eq("_id", map.getId());
            var result = collection().replaceOne(filter, map);
            if (result.getModifiedCount() == 0)
                throw NOT_FOUND;
            return null;
        }, ForkJoinPool.commonPool());
    }

    private @NotNull MongoCollection<MapData> collection() {
        return client.getDatabase(DB_NAME).getCollection("maps", MapData.class);
    }
}
