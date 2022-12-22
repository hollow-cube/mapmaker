package net.hollowcube.mapmaker.storage;

import com.mongodb.DuplicateKeyException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import net.hollowcube.mapmaker.model.PlayerData;
import net.hollowcube.mapmaker.result.FutureResult;
import net.hollowcube.mapmaker.result.Result;
import org.jetbrains.annotations.NotNull;

import static com.mongodb.client.model.Filters.eq;

public class PlayerStorageMongo implements PlayerStorage {
    private static final String DB_NAME = System.getProperty("mongo.db", "mapmaker");

    private final MongoClient client;

    public PlayerStorageMongo(@NotNull MongoClient client) {
        this.client = client;
    }

    @Override
    public @NotNull FutureResult<@NotNull PlayerData> createPlayer(@NotNull PlayerData player) {
        return FutureResult.supply(() -> {
            try {
                collection().insertOne(player);
            } catch (DuplicateKeyException ignored) {
                return Result.error(ERR_DUPLICATE_ENTRY);
            }
            return Result.of(player);
        });
    }

    @Override
    public @NotNull FutureResult<@NotNull PlayerData> getPlayerById(@NotNull String id) {
        return FutureResult.supply(() -> {
            var filter = eq("_id", id);
            var result = collection().find(filter).limit(1).first();
            if (result == null)
                return Result.error(ERR_NOT_FOUND);
            return Result.of(result);
        });
    }

    @Override
    public @NotNull FutureResult<@NotNull PlayerData> getPlayerByUuid(@NotNull String uuid) {
        return FutureResult.supply(() -> {
            var filter = eq("uuid", uuid);
            var result = collection().find(filter).limit(1).first();
            if (result == null)
                return Result.error(ERR_NOT_FOUND);
            return Result.of(result);
        });
    }

    @Override
    public @NotNull FutureResult<@NotNull Void> updatePlayer(@NotNull PlayerData player) {
        return FutureResult.supply(() -> {
            var filter = eq("_id", player.getId());
            var result = collection().replaceOne(filter, player);
            if (result.getModifiedCount() == 0)
                return Result.error(ERR_NOT_FOUND);
            return Result.ofNull();
        });
    }

    private @NotNull MongoCollection<PlayerData> collection() {
        return client.getDatabase(DB_NAME).getCollection("players", PlayerData.class);
    }
}
