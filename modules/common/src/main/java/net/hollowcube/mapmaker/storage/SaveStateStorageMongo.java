package net.hollowcube.mapmaker.storage;

import com.mongodb.DuplicateKeyException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import net.hollowcube.mapmaker.model.SaveState;
import net.hollowcube.mapmaker.result.FutureResult;
import net.hollowcube.mapmaker.result.Result;
import org.jetbrains.annotations.NotNull;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Sorts.descending;

public class SaveStateStorageMongo implements SaveStateStorage {
    private static final String DB_NAME = System.getProperty("mongo.db", "mapmaker");

    private final MongoClient client;

    public SaveStateStorageMongo(@NotNull MongoClient client) {
        this.client = client;
    }

    @Override
    public @NotNull FutureResult<@NotNull SaveState> createSaveState(@NotNull SaveState saveState) {
        return FutureResult.supply(() -> {
            try {
                collection().insertOne(saveState);
            } catch (DuplicateKeyException ignored) {
                return Result.error(ERR_DUPLICATE_ENTRY);
            }
            return Result.of(saveState);
        });
    }

    @Override
    public @NotNull FutureResult<Void> updateSaveState(@NotNull SaveState saveState) {
        return FutureResult.supply(() -> {
            var filter = eq("_id", saveState.getId());
            var result = collection().replaceOne(filter, saveState);
            if (result.getModifiedCount() == 0)
                return Result.error(ERR_NOT_FOUND);
            return Result.ofNull();
        });
    }

    @Override
    public @NotNull FutureResult<@NotNull SaveState> getLatestSaveState(@NotNull String playerId, @NotNull String mapId) {
        return FutureResult.supply(() -> {
            var filter = and(
                    eq("player_id", playerId),
                    eq("map_id", mapId),
                    or(eq("complete", false), not(exists("complete"))));
            var sort = descending("start_time");
            var result = collection().find(filter, SaveState.class).sort(sort).limit(1).first();
            if (result == null)
                return Result.error(ERR_NOT_FOUND);
            return Result.of(result);
        });
    }

    private @NotNull MongoCollection<SaveState> collection() {
        return client.getDatabase(DB_NAME).getCollection("savestates", SaveState.class);
    }
}
