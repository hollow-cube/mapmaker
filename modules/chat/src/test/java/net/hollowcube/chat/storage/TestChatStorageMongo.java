package net.hollowcube.chat.storage;

import net.hollowcube.chat.ChatQuery;
import org.bson.BsonDocument;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

public class TestChatStorageMongo {


    public static class TestQueryConversion {

        @SuppressWarnings("DataFlowIssue") // Breaks contract, but we are only calling chatQueryToBson
        private final ChatStorageMongo storage = new ChatStorageMongo(null, null);

        @Test
        public void testEmptyQuery() {
            ChatQuery chatQuery = ChatQuery.builder().build();
            BsonDocument query = storage.chatQueryToBson(chatQuery).toBsonDocument();

            assertThat(query).isEmpty();
        }

        @Test
        public void testFilterServerIds() {
            ChatQuery chatQuery = ChatQuery.builder()
                    .serverId("a", "b")
                    .build();
            BsonDocument query = storage.chatQueryToBson(chatQuery).toBsonDocument();

            BsonDocument expected = BsonDocument.parse("""
                    {
                        "$and": [{
                            $or: [
                                {
                                    "serverId": {
                                        "$regularExpression": {"pattern": "a*", "options": ""}
                                    }
                                },
                                {
                                    "serverId": {
                                        "$regularExpression": {"pattern": "b*", "options": ""}
                                    }
                                }
                            ]
                        }]
                    }
                    """);
            assertThat(query).isEqualTo(expected);
        }

        @Test
        public void testFilterChannelIds() {
            ChatQuery chatQuery = ChatQuery.builder()
                    .context("1", "2")
                    .build();
            BsonDocument query = storage.chatQueryToBson(chatQuery).toBsonDocument();

            BsonDocument expected = BsonDocument.parse("""
                    {
                        "$and": [{
                            "context": {
                                "$in": ["1", "2"]
                            }
                        }]
                    }
                    """);
            assertThat(query).isEqualTo(expected);
        }

        @Test
        public void testFilterSenders() {
            ChatQuery chatQuery = ChatQuery.builder()
                    .sender("19aa5eff-0f80-464d-b7c1-330b729571c8")
                    .sender("c2ead875-fd86-45bd-991a-24563d1defd2")
                    .build();
            BsonDocument query = storage.chatQueryToBson(chatQuery).toBsonDocument();

            BsonDocument expected = BsonDocument.parse("""
                    {
                        "$and": [{
                            "sender": {
                                "$in": [
                                    "19aa5eff-0f80-464d-b7c1-330b729571c8",
                                    "c2ead875-fd86-45bd-991a-24563d1defd2"
                                ]
                            }
                        }]
                    }
                    """);
            assertThat(query).isEqualTo(expected);
        }

        @Test
        public void testFilterMessage() {
            ChatQuery chatQuery = ChatQuery.builder()
                    .message("Hello, world")
                    .build();
            BsonDocument query = storage.chatQueryToBson(chatQuery).toBsonDocument();

            BsonDocument expected = BsonDocument.parse("""
                    {
                        "$and": [{
                            "message": "Hello, world"
                        }]
                    }
                    """);
            assertThat(query).isEqualTo(expected);
        }
    }
}
