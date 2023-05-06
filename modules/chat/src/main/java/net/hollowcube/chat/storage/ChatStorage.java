package net.hollowcube.chat.storage;

import net.hollowcube.chat.ChatMessage;
import net.hollowcube.chat.ChatQuery;
import net.hollowcube.common.config.MongoConfig;
import net.hollowcube.mapmaker.storage.client.MongoClientFactory;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ChatStorage {
    static @NotNull ChatStorage memory() {
        return new ChatStorageMemory();
    }

    static @Blocking
    @NotNull ChatStorage mongo(@NotNull MongoConfig config) {
        var client = MongoClientFactory.get().newClient(config);
        return new ChatStorageMongo(client, config);
    }

    /**
     * Record a chat message to storage. How/when/if the message is written is up to the implementing class, but may not
     * block the current thread to do so.
     *
     * @param message The chat message to save
     */
    @Blocking
    void recordChatMessage(@NotNull ChatMessage message);

    @Blocking
    @NotNull List<ChatMessage> queryChatMessages(@NotNull ChatQuery query);

}
