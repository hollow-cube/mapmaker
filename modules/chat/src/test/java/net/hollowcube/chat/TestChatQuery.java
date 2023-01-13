package net.hollowcube.chat;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestChatQuery {

    @Test
    public void testBuilderMethods() {
        String[] uuids = new String[]{new UUID(0, 0).toString(), new UUID(0, 1).toString(), new UUID(0, 2).toString(), new UUID(0, 3).toString()};
        ChatQuery query = ChatQuery.builder()
                .serverId("a", "b")
                .serverIds(List.of("c", "d"))
                .context("1", "2")
                .contexts(List.of("3", "4"))
                .sender(uuids[0], uuids[1])
                .senders(List.of(uuids[2], uuids[3]))
                .message("test message")
                .build();

        assertThat(query.serverIds()).containsExactly("a", "b", "c", "d");
        assertThat(query.contexts()).containsExactly("1", "2", "3", "4");
        assertThat(query.senders()).containsExactly((Object[]) uuids);
        assertThat(query.message()).isEqualTo("test message");
    }

    @Test
    public void testInvalidServerId() {
        ChatQuery.Builder query = ChatQuery.builder()
                .serverId("*");

        var exc = assertThrows(IllegalArgumentException.class, query::build);
        assertThat(exc.getMessage()).isEqualTo("Illegal character in serverId '*'");
    }

}
