package net.hollowcube.chat.storage;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import net.hollowcube.chat.ChatMessage;
import net.hollowcube.chat.ChatQuery;
import net.hollowcube.config.ConfigProvider;
import net.hollowcube.dfu.ExtraCodecs;
import net.hollowcube.mongo.MongoConfig;
import org.bson.UuidRepresentation;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ChatStorage {
    static @NotNull ChatStorage noop() {
        return new NoopChatStorage();
    }

    static @NotNull ChatStorage mongo(MongoClient client) {
        return new MongoChatStorage(client);
    }

    static @NotNull ChatStorage fromConfig() {
        Logger logger = LoggerFactory.getLogger(ChatStorage.class);

        enum Type {NOOP, MONGO}
        Type type = ConfigProvider.load("chat_storage_type", ExtraCodecs.forEnum(Type.class).orElse(Type.NOOP));
        logger.info("Using {} chat storage", type);

        return switch (type) {
            case NOOP -> noop();
            case MONGO -> {
                //todo need to have a common mongo client somewhere, not worth recreating every time
                MongoConfig config = ConfigProvider.load("mongo", MongoConfig.CODEC);
                MongoClient client = MongoClients.create(MongoClientSettings.builder()
                        .applyConnectionString(new ConnectionString(config.uri()))
                        .uuidRepresentation(UuidRepresentation.STANDARD)
                        .build());
                yield mongo(client);
            }
        };
    }


    /**
     * Record a chat message to storage. How/when/if the message is written is up to the implementing class, but may not
     * block the current thread to do so.
     *
     * @param message The chat message to save
     */
    CompletableFuture<Void> recordChatMessage(@NotNull ChatMessage message);

    CompletableFuture<List<ChatMessage>> queryChatMessages(@NotNull ChatQuery query);

}
