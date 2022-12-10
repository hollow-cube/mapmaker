package net.hollowcube.mapmaker.storage;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import net.hollowcube.mapmaker.model.MapData;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

import static com.mongodb.client.model.Filters.eq;

public class MapStorageMongo implements MapStorage {
    private static final String DB_NAME = System.getProperty("mongo.db", "mapmaker");
    private static final String OWNER_NAME_INDEX_NAME = "owner_name_unique";

    private final MongoClient client;

    public MapStorageMongo(@NotNull MongoClient client) {
        this.client = client;

        var indexKeys = new Document();
        indexKeys.append("owner", 1);
        indexKeys.append("name", 1);
        collection().createIndex(indexKeys, new IndexOptions().unique(true).name(OWNER_NAME_INDEX_NAME));
    }

    @Override
    public @NotNull CompletableFuture<MapData> createMap(@NotNull MapData map) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                collection().insertOne(map);
            } catch (MongoWriteException err) {
                if (err.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
                    // This is a pretty cursed way to check for this error. Mongo does not seem to inform which key
                    // or index caused the error (in a raw form), so we just look for the index name in the error message.
                    if (err.getError().getMessage().contains(OWNER_NAME_INDEX_NAME))
                        throw ERR_DUPLICATE_NAME;

                    // ID mismatch
                    throw ERR_DUPLICATE_ENTRY;
                }
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
                throw ERR_NOT_FOUND;
            return result;
        }, ForkJoinPool.commonPool());
    }

    @Override
    public @NotNull CompletableFuture<Void> updateMap(@NotNull MapData map) {
        return CompletableFuture.supplyAsync(() -> {
            var filter = eq("_id", map.getId());
            var result = collection().replaceOne(filter, map);
            if (result.getModifiedCount() == 0)
                throw ERR_NOT_FOUND;
            return null;
        }, ForkJoinPool.commonPool());
    }

    private @NotNull MongoCollection<MapData> collection() {
        return client.getDatabase(DB_NAME).getCollection("maps", MapData.class);
    }
}
