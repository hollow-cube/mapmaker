package net.hollowcube.chat.storage;

import net.hollowcube.chat.ChatMessage;
import net.hollowcube.chat.ChatQuery;
import net.hollowcube.common.result.FutureResult;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ChatStorage {
    static @NotNull ChatStorage noop() {
        return new ChatStorageNoop();
    }

    static @NotNull ChatStorage mongo(@NotNull String mongoUri) {
        return new ChatStorageMongo(MongoUtil.getClient(mongoUri));
    }

    /**
     * Record a chat message to storage. How/when/if the message is written is up to the implementing class, but may not
     * block the current thread to do so.
     *
     * @param message The chat message to save
     */
    @NotNull FutureResult<Void> recordChatMessage(@NotNull ChatMessage message);

    @NotNull FutureResult<List<ChatMessage>> queryChatMessages(@NotNull ChatQuery query);

}
