package net.hollowcube.mapmaker.storage;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.result.FutureResult;
import net.hollowcube.mapmaker.result.Result;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.inc;

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
    public @NotNull FutureResult<MapData> createMap(@NotNull MapData map) {
        return FutureResult.supply(() -> {
            try {
                collection().insertOne(map);
            } catch (MongoWriteException err) {
                if (err.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
                    // This is a pretty cursed way to check for this error. Mongo does not seem to inform which key
                    // or index caused the error (in a raw form), so we just look for the index name in the error message.
                    if (err.getError().getMessage().contains(OWNER_NAME_INDEX_NAME))
                        return Result.error(ERR_DUPLICATE_NAME);

                    // ID mismatch
                    return Result.error(ERR_DUPLICATE_ENTRY);
                }
            }
            return Result.of(map);
        });
    }

    @Override
    public @NotNull FutureResult<MapData> getMapById(@NotNull String mapId) {
        return FutureResult.supply(() -> {
            var filter = eq("_id", mapId);
            var result = collection().find(filter).limit(1).first();
            if (result == null)
                return Result.error(ERR_NOT_FOUND);
            return Result.of(result);
        });
    }

    @Override
    public @NotNull FutureResult<Void> updateMap(@NotNull MapData map) {
        return FutureResult.supply(() -> {
            var filter = eq("_id", map.getId());
            var result = collection().replaceOne(filter, map);
            if (result.getModifiedCount() == 0)
                return Result.error(ERR_NOT_FOUND);
            return Result.of(null);
        });
    }

    @Override
    public @NotNull FutureResult<List<MapData>> getMapsByPlayer(@NotNull String playerId) {
        return FutureResult.supply(() -> {
            var filter = eq("owner", playerId);
            var result = collection().find(filter).into(new ArrayList<>());
            return Result.of(result);
        });
    }

    @Override
    public @NotNull FutureResult<MapData> getPlayerMap(@NotNull String playerId, @NotNull String nameOrId) {
        return FutureResult.supply(() -> {
            var filter = and(eq("owner", playerId),
                    or(eq("_id", nameOrId), eq("name", nameOrId)));
            var result = collection().find(filter).limit(1).first();
            if (result == null)
                return Result.error(ERR_NOT_FOUND);
            return Result.of(result);
        });
    }

    public @NotNull FutureResult<String> getNextId() {
        return FutureResult.supply(() -> {
            var filter = new Document();
            var update = inc("nextId", 1);
            var result = shortIdCollection().findOneAndUpdate(filter, update);
            if (result == null) {
                // Document does not exist
                shortIdCollection().insertOne(new Document("nextId", 1));
                return Result.of("00001");
            }
            var n = result.getInteger("nextId");
            var id = "00000" + Integer.toString(n, 36);
            return Result.of(id.substring(id.length() - 5));
        });
    }

    private @NotNull MongoCollection<MapData> collection() {
        return client.getDatabase(DB_NAME).getCollection("maps", MapData.class);
    }

    private @NotNull MongoCollection<Document> shortIdCollection() {
        return client.getDatabase(DB_NAME).getCollection("id_inc");
    }
}
