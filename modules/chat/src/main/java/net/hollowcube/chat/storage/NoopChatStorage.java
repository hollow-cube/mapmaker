package net.hollowcube.chat.storage;

import net.hollowcube.chat.ChatMessage;
import net.hollowcube.chat.ChatQuery;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Dummy chat storage which does not do anything with the messages.
 */
class NoopChatStorage implements ChatStorage {

    @Override
    public CompletableFuture<Void> recordChatMessage(@NotNull ChatMessage message) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<List<ChatMessage>> queryChatMessages(@NotNull ChatQuery query) {
        return CompletableFuture.completedFuture(List.of());
    }
}
