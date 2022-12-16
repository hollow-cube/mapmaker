package net.hollowcube.mapmaker.storage;

import com.mongodb.DuplicateKeyException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import net.hollowcube.mapmaker.model.PlayerData;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

import static com.mongodb.client.model.Filters.eq;

public class PlayerStorageMongo implements PlayerStorage {
    private static final String DB_NAME = System.getProperty("mongo.db", "mapmaker");

    private final MongoClient client;

    public PlayerStorageMongo(@NotNull MongoClient client) {
        this.client = client;
    }

    @Override
    public @NotNull CompletableFuture<@NotNull PlayerData> createPlayer(@NotNull PlayerData player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                collection().insertOne(player);
            } catch (DuplicateKeyException ignored) {
                throw DUPLICATE_ENTRY;
            }
            return player;
        }, ForkJoinPool.commonPool());
    }

    @Override
    public @NotNull CompletableFuture<@NotNull PlayerData> getPlayerById(@NotNull String id) {
        return CompletableFuture.supplyAsync(() -> {
            var filter = eq("_id", id);
            var result = collection().find(filter).limit(1).first();
            if (result == null)
                throw NOT_FOUND;
            return result;
        }, ForkJoinPool.commonPool());
    }

    @Override
    public @NotNull CompletableFuture<@NotNull PlayerData> getPlayerByUuid(@NotNull String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            var filter = eq("uuid", uuid);
            var result = collection().find(filter).limit(1).first();
            if (result == null)
                throw NOT_FOUND;
            return result;
        }, ForkJoinPool.commonPool());
    }

    private @NotNull MongoCollection<PlayerData> collection() {
        return client.getDatabase(DB_NAME).getCollection("players", PlayerData.class);
    }
}
