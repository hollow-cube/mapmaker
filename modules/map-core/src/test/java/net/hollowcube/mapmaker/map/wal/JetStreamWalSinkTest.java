package net.hollowcube.mapmaker.map.wal;

import io.nats.client.api.PublishAck;
import io.nats.client.impl.Headers;
import net.hollowcube.mapmaker.api.maps.MapClient;
import net.hollowcube.mapmaker.util.nats.JetStreamPublisher;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JetStreamWalSinkTest {
    private record Published(String subject, Headers headers, byte[] body) {}

    private final List<Published> published = new ArrayList<>();
    private final List<byte[]> uploaded = new ArrayList<>();
    private int failures;

    private JetStreamWalSink sink() {
        return new JetStreamWalSink(
            new JetStreamPublisher() {
                @Override
                public PublishAck publish(String subject, Object payload) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public @Nullable PublishAck publish(String subject, Headers headers, byte[] body) {
                    if (failures > 0) {
                        failures--;
                        throw new IllegalStateException("no ack");
                    }
                    published.add(new Published(subject, headers, body));
                    return null;
                }
            },
            new MapClient() {
                @Override
                public String putWalBlob(String mapId, String worldId, byte[] batch) {
                    uploaded.add(batch);
                    return "abc123";
                }
            },
            "map", "world"
        );
    }

    @Test
    void publishesASmallBatchInTheMessage() throws Exception {
        sink().accept(new WalWriter.Batch(5, 6, 1_000, 2_000, new byte[] {1, 2, 3}));

        var message = published.getFirst();
        assertEquals("map-wal.map", message.subject());
        assertArrayEquals(new byte[] {1, 2, 3}, message.body());
        assertEquals("map:5", message.headers().getFirst("Nats-Msg-Id"));
        assertEquals("world", message.headers().getFirst(JetStreamWalSink.HEADER_WORLD));
        assertEquals("5", message.headers().getFirst(JetStreamWalSink.HEADER_FIRST_SEQ));
        assertEquals("6", message.headers().getFirst(JetStreamWalSink.HEADER_LAST_SEQ));
        assertEquals("1000", message.headers().getFirst(JetStreamWalSink.HEADER_FIRST_AT));
        assertEquals("2000", message.headers().getFirst(JetStreamWalSink.HEADER_LAST_AT));
        assertEquals("3", message.headers().getFirst(JetStreamWalSink.HEADER_LENGTH));
        assertNull(message.headers().getFirst(JetStreamWalSink.HEADER_OBJECT));
        assertTrue(uploaded.isEmpty());
    }

    @Test
    void uploadsABatchTooLargeForAMessage() throws Exception {
        var bytes = new byte[JetStreamWalSink.MAX_INLINE_BYTES + 1];
        sink().accept(new WalWriter.Batch(1, 1, 0, 0, bytes));

        assertSame(bytes, uploaded.getFirst());
        var message = published.getFirst();
        assertEquals(0, message.body().length);
        assertEquals("abc123", message.headers().getFirst(JetStreamWalSink.HEADER_OBJECT));
        assertEquals(Integer.toString(bytes.length), message.headers().getFirst(JetStreamWalSink.HEADER_LENGTH));
    }

    @Test
    void retriesThenGivesUp() {
        failures = 1;
        assertDoesNotThrow(() -> sink().accept(new WalWriter.Batch(1, 1, 0, 0, new byte[] {1})));
        assertEquals(1, published.size());

        failures = 4;
        assertThrows(IllegalStateException.class, () -> sink().accept(new WalWriter.Batch(2, 2, 0, 0, new byte[] {1})));
        assertEquals(1, published.size());
    }
}
