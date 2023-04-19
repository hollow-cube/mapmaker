package net.hollowcube.mapmaker.storage;

import com.google.common.util.concurrent.Futures;
import com.mongodb.DuplicateKeyException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import net.hollowcube.common.config.MongoConfig;
import net.minestom.server.entity.Player;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

public class BanStorageMongo implements BanStorage {


    private final MongoClient client;
    private final MongoConfig config;

    public BanStorageMongo(@NotNull MongoClient client, @NotNull MongoConfig config) {
        this.client = client;
        this.config = config;
    }

    @Override
    public @NotNull Future<Boolean> isPlayerBanned(@NotNull Player player) {
        return Futures.submit(() -> {
            FindIterable<Document> documents = collection().find(Filters.eq("uuid", player.getUuid().toString()));
            return documents.first() != null;
        }, ForkJoinPool.commonPool());
    }

    public @NotNull Future<Void> banPlayer(@NotNull Player player) {
        return Futures.submit(() -> {
            try {
                Document id = new Document();
                id.append("uuid", player.getUuid().toString());
                id.append("username", player.getUsername());
                collection().insertOne(id);
            } catch (DuplicateKeyException ignored) {

            }
        }, ForkJoinPool.commonPool());
    }

    @Override
    public @NotNull Future<Void> unbanPlayer(@NotNull String username) {
        return Futures.submit(() -> {
            collection().deleteOne(Filters.eq("username", username));
        }, ForkJoinPool.commonPool());
    }

    @Override
    public @NotNull Future<Collection<String>> getUnbannedUsernames() {
        return Futures.submit(() -> {
            List<String> usernames = new ArrayList<>();
            for (Document document : collection().find()) {
                usernames.add(document.getString("username"));
            }
            return usernames;
        }, ForkJoinPool.commonPool());
    }

    private @NotNull MongoCollection<Document> collection() {
        return client.getDatabase(config.database()).getCollection("bans");
    }
}
