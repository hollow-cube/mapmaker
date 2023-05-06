package net.hollowcube.mapmaker.storage;

import com.mongodb.DuplicateKeyException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import net.hollowcube.common.config.MongoConfig;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import static com.mongodb.client.model.Filters.eq;

public class WhitelistStorageMongo implements WhitelistStorage {
    private final MongoClient client;
    private final MongoConfig config;

    public WhitelistStorageMongo(@NotNull MongoClient client, @NotNull MongoConfig config) {
        this.client = client;
        this.config = config;
    }

    @Override
    public boolean isWhitelisted(@NotNull String playerId) {
        FindIterable<Document> documents = collection().find(eq("uuid", playerId));
        return documents.first() != null;
    }

    @Override
    public void addToWhitelist(@NotNull String playerId) {
        try {
            Document id = new Document();
            id.append("uuid", playerId);
            collection().insertOne(id);
        } catch (DuplicateKeyException ignored) {
            // do nothing, they are already on the whitelist
        }
    }

    @Override
    public void removeFromWhitelist(@NotNull String playerId) {
        collection().deleteOne(eq("uuid", playerId));
    }

    private @NotNull MongoCollection<Document> collection() {
        return client.getDatabase(config.database()).getCollection("whitelist");
    }
}
