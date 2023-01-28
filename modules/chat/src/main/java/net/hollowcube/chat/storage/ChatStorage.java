package net.hollowcube.chat.storage;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import net.hollowcube.chat.ChatMessage;
import net.hollowcube.chat.ChatQuery;
import net.hollowcube.common.config.MongoConfig;
import net.hollowcube.mapmaker.storage.client.MongoClientFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ChatStorage {
    static @NotNull ChatStorage memory() {
        return new ChatStorageMemory();
    }

    static @NotNull ListenableFuture<ChatStorage> mongo(@NotNull MongoConfig config) {
        var clientFactory = MongoClientFactory.get();
        return Futures.transform(
                clientFactory.newClient(config),
                client -> new ChatStorageMongo(client, config),
                Runnable::run
        );
    }

    /**
     * Record a chat message to storage. How/when/if the message is written is up to the implementing class, but may not
     * block the current thread to do so.
     *
     * @param message The chat message to save
     */
    @NotNull ListenableFuture<Void> recordChatMessage(@NotNull ChatMessage message);

    @NotNull ListenableFuture<@NotNull List<ChatMessage>> queryChatMessages(@NotNull ChatQuery query);

}
