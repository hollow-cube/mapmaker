package net.hollowcube.chat.storage;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import net.hollowcube.chat.ChatMessage;
import net.hollowcube.chat.ChatQuery;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Dummy chat storage which does not do anything with the messages.
 */
class ChatStorageMemory implements ChatStorage {
    private static final System.Logger logger = System.getLogger(ChatStorageMemory.class.getName());

    //todo make this actually store messages

    @Override
    public @NotNull ListenableFuture<Void> recordChatMessage(@NotNull ChatMessage message) {
        return Futures.immediateVoidFuture();
    }

    @Override
    public @NotNull ListenableFuture<List<ChatMessage>> queryChatMessages(@NotNull ChatQuery query) {
        logger.log(System.Logger.Level.INFO, "Attempted to query chat messages, but the method is not implemented.");
        return Futures.immediateFuture(List.of());
    }
}
