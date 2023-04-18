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

import java.util.UUID;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

public class BasicUUIDStorageMongo {

    private final MongoClient client;
    private final MongoConfig config;
    private final String collectionName;

    public BasicUUIDStorageMongo(@NotNull MongoClient client, @NotNull MongoConfig config, @NotNull String collectionName) {
        this.client = client;
        this.config = config;
        this.collectionName = collectionName;
    }

    protected @NotNull Future<Boolean> isPlayerStored(@NotNull Player player) {
       return isUUIDStored(player.getUuid());
    }

    protected @NotNull Future<Boolean> isUUIDStored(@NotNull UUID uuid) {
        return Futures.submit(() -> {
            FindIterable<Document> documents = collection().find(Filters.eq("uuid", uuid.toString()));
            return documents.first() != null;
        }, ForkJoinPool.commonPool());
    }

    protected @NotNull Future<Void> addPlayerToStore(@NotNull Player player) {
        return addUUIDToStore(player.getUuid());
    }

    protected @NotNull Future<Void> addUUIDToStore(@NotNull UUID uuid) {
        return Futures.submit(() -> {
            try {
                Document id = new Document();
                id.append("uuid", uuid.toString());
                collection().insertOne(id);
            } catch (DuplicateKeyException ignored) {

            }
        }, ForkJoinPool.commonPool());
    }

    protected @NotNull Future<Void> removePlayerFromStore(@NotNull Player player) {
        return removeUUIDFromStore(player.getUuid());
    }

    protected @NotNull Future<Void> removeUUIDFromStore(@NotNull UUID uuid) {
        return Futures.submit(() -> {
            collection().deleteOne(Filters.eq("uuid", uuid.toString()));
        }, ForkJoinPool.commonPool());
    }

    private @NotNull MongoCollection<Document> collection() {
        return client.getDatabase(config.database()).getCollection(collectionName);
    }

    // TODO Can we do something with this?
    // private final UuidCodecProvider uuidCodec = new UuidCodecProvider(UuidRepresentation.STANDARD);
}
