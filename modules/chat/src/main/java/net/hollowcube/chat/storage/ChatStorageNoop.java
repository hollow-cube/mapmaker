package net.hollowcube.chat.storage;

import net.hollowcube.chat.ChatMessage;
import net.hollowcube.chat.ChatQuery;
import net.hollowcube.common.result.FutureResult;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Dummy chat storage which does not do anything with the messages.
 */
class ChatStorageNoop implements ChatStorage {

    @Override
    public @NotNull FutureResult<Void> recordChatMessage(@NotNull ChatMessage message) {
        return FutureResult.ofNull();
    }

    @Override
    public @NotNull FutureResult<List<ChatMessage>> queryChatMessages(@NotNull ChatQuery query) {
        return FutureResult.of(List.of());
    }
}
