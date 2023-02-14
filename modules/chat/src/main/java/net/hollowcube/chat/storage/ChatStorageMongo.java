package net.hollowcube.chat.storage;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import net.hollowcube.chat.ChatMessage;
import net.hollowcube.chat.ChatQuery;
import net.hollowcube.common.config.MongoConfig;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.excludeId;
import static com.mongodb.client.model.Sorts.descending;

class ChatStorageMongo implements ChatStorage {
    static final String CHAT_COLLECTION = "chat";

    private static final int CHAT_QUERY_MAX_RESULT_WINDOW = 15;

    private final MongoClient client;
    private final MongoConfig config;

    public ChatStorageMongo(@NotNull MongoClient client, @NotNull MongoConfig config) {
        this.client = client;
        this.config = config;
    }

    @Override
    public @NotNull ListenableFuture<Void> recordChatMessage(@NotNull ChatMessage message) {
        return Futures.submit(() -> {
            collection().insertOne(message);
        }, ForkJoinPool.commonPool());
    }

    @Override
    public @NotNull ListenableFuture<List<ChatMessage>> queryChatMessages(@NotNull ChatQuery query) {
        return Futures.submit(() -> {
            List<ChatMessage> results = new ArrayList<>();
            collection().find(chatQueryToBson(query))
                    .projection(excludeId())
                    .sort(descending("timestamp"))
                    .limit(CHAT_QUERY_MAX_RESULT_WINDOW)
                    .into(results);
            return List.copyOf(results);
        }, ForkJoinPool.commonPool());
    }

    /**
     * Convert a chat query to a bson query for mongodb.
     *
     * @param query a chat query to convert to bson
     * @return A BSON query of the given {@link ChatQuery}.
     */
    public @NotNull Bson chatQueryToBson(@NotNull ChatQuery query) {
        List<Bson> conditions = new ArrayList<>();

        // Currently the generated query uses the form
        // { $and: [ { <field>: { $in: <values> } } ], ... }
        // but an optimization would be to use the form
        // { $and: [ { <field>: <value> }, ... ]
        // if there is only a single query value.

        if (!query.serverIds().isEmpty()) {
            // Server ID matches anything starting with the provided string.
            // $or the provided strings with a trailing '*' as regex
            conditions.add(or(query.serverIds().stream().map(str -> regex("serverId", str + "*")).toList()));
        }

        if (!query.contexts().isEmpty())
            conditions.add(in("context", query.contexts()));

        if (!query.senders().isEmpty())
            conditions.add(in("sender", query.senders()));

        if (query.message() != null)
            conditions.add(eq("message", query.message()));

        // Return all conditions $and-ed together, or an empty query if there are no conditions
        // Must check this because mongo does not allow an empty $and
        if (conditions.isEmpty()) return new Document();
        return and(conditions);
    }

    private MongoCollection<ChatMessage> collection() {
        return client.getDatabase(config.database()).getCollection(CHAT_COLLECTION, ChatMessage.class);
    }
}
