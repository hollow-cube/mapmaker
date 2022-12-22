package net.hollowcube.chat;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public record ChatQuery(
        //todo timestamps
        @NotNull List<String> serverIds,
        @NotNull List<String> contexts,
        @NotNull List<UUID> senders,
        @Nullable String message
) {
    public static final Predicate<String> SERVER_ID_REGEX = Pattern.compile("[a-zA-Z0-9_-]*").asMatchPredicate();

    public static @NotNull Builder builder() {
        return new Builder();
    }

    public ChatQuery {
        serverIds = List.copyOf(serverIds);
        contexts = List.copyOf(contexts);
        senders = List.copyOf(senders);

        // Only allow alphanumeric characters in server IDs
        for (String serverId : serverIds) {
            if (SERVER_ID_REGEX.test(serverId))
                continue;
            throw new IllegalArgumentException("Illegal character in serverId '" + serverId + "'");
        }
    }


    public static class Builder {
        private final List<String> serverIds = new ArrayList<>();
        private final List<String> channelIds = new ArrayList<>();
        private final List<UUID> senders = new ArrayList<>();
        private String message = null;

        private Builder() {}

        @Contract("_ -> this")
        public @NotNull Builder context(String... context) {
            return this.contexts(Arrays.asList(context));
        }

        @Contract("_ -> this")
        public @NotNull Builder contexts(List<String> contexts) {
            this.channelIds.addAll(contexts);
            return this;
        }

        @Contract("_ -> this")
        public @NotNull Builder serverId(String... serverIds) {
            return this.serverIds(Arrays.asList(serverIds));
        }

        @Contract("_ -> this")
        public @NotNull Builder serverIds(List<String> serverIds) {
            this.serverIds.addAll(serverIds);
            return this;
        }

        @Contract("_ -> this")
        public @NotNull Builder sender(UUID... senders) {
            return this.senders(Arrays.asList(senders));
        }

        @Contract("_ -> this")
        public @NotNull Builder senders(List<UUID> senders) {
            this.senders.addAll(senders);
            return this;
        }

        @Contract("_ -> this")
        public @NotNull Builder message(String message) {
            this.message = message;
            return this;
        }

        public @NotNull ChatQuery build() {
            return new ChatQuery(serverIds, channelIds, senders, message);
        }
    }
}
