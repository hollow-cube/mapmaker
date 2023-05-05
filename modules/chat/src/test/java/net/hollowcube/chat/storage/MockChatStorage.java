package net.hollowcube.chat.storage;

import com.google.common.util.concurrent.Futures;
import net.hollowcube.chat.ChatMessage;
import net.hollowcube.chat.ChatQuery;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public record MockChatStorage(
        @NotNull List<ChatMessage> messages,
        @NotNull List<ChatQuery> queries
) implements ChatStorage {

    public MockChatStorage() {
        this(new ArrayList<>(), new ArrayList<>());
    }

    @Override
    public @NotNull ListenableFuture<Void> recordChatMessage(@NotNull ChatMessage message) {
        messages.add(message);
        return Futures.immediateVoidFuture();
    }

    @Override
    public @NotNull ListenableFuture<List<ChatMessage>> queryChatMessages(@NotNull ChatQuery query) {
        queries.add(query);
        return Futures.immediateFuture(List.of());
    }

    public ChatMessage assertOneMessage() {
        assertEquals(1, messages.size());
        return messages.get(0);
    }

    public void assertEmpty() {
        assertEquals(0, messages.size());
    }
}
