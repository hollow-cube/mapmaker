package net.hollowcube.mapmaker.storage;

import com.google.common.util.concurrent.Futures;
import com.mongodb.DuplicateKeyException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import net.hollowcube.common.config.MongoConfig;
import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.codecs.UuidCodecProvider;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

public class WhitelistStorageMongo implements WhitelistStorage {

    private final MongoClient client;
    private final MongoConfig config;

    public WhitelistStorageMongo(@NotNull MongoClient client, @NotNull MongoConfig config) {
        this.client = client;
        this.config = config;
    }

    @Override
    public @NotNull Future<Boolean> isUUIDWhitelisted(@NotNull UUID uuid) {
        return Futures.submit(() -> {
            FindIterable<Document> documents = collection().find(Filters.eq("uuid", uuid));
            return documents.first() != null;
        }, ForkJoinPool.commonPool());
    }

    @Override
    public @NotNull Future<Void> addToWhitelist(@NotNull UUID uuid) {
        return Futures.submit(() -> {
            try {
                Document id = new Document();
                id.append("uuid", uuid);
                collection().insertOne(id);
            } catch (DuplicateKeyException ignored) {

            }
        }, ForkJoinPool.commonPool());
    }

    @Override
    public @NotNull Future<Void> removeFromWhitelist(@NotNull UUID uuid) {
        return Futures.submit(() -> {
            collection().deleteOne(Filters.eq("uuid", uuid));
        }, ForkJoinPool.commonPool());
    }

    private @NotNull MongoCollection<Document> collection() {
        return client.getDatabase(config.database()).getCollection("uuids");
    }


    private final UuidCodecProvider uuidCodec = new UuidCodecProvider(UuidRepresentation.STANDARD);
}
