package net.hollowcube.mapmaker.map.wal;

import io.nats.client.impl.Headers;
import net.hollowcube.mapmaker.api.HttpClientWrapper;
import net.hollowcube.mapmaker.api.maps.MapClient;
import net.hollowcube.mapmaker.util.nats.JetStreamPublisher;

import java.util.function.Consumer;

/// Publishes each batch of one map's log on `map-wal.{mapId}`, waiting for the stream to acknowledge it.
///
/// A batch too large for one message is uploaded first and published as an empty message naming it.
public final class JetStreamWalSink implements Consumer<WalWriter.Batch> {
    public static final String HEADER_WORLD = "Hc-Wal-World";
    public static final String HEADER_FIRST_SEQ = "Hc-Wal-First-Seq";
    public static final String HEADER_LAST_SEQ = "Hc-Wal-Last-Seq";
    public static final String HEADER_FIRST_AT = "Hc-Wal-First-At";
    public static final String HEADER_LAST_AT = "Hc-Wal-Last-At";
    public static final String HEADER_LENGTH = "Hc-Wal-Length";
    public static final String HEADER_OBJECT = "Hc-Wal-Object";
    private static final String HEADER_MSG_ID = "Nats-Msg-Id";

    /// The broker refuses a message over 1 MiB, headers included.
    static final int MAX_INLINE_BYTES = 512 * 1024;
    private static final long[] RETRY_DELAYS_MILLIS = {250, 1_000, 3_000};
    private static final byte[] EMPTY = new byte[0];

    private final JetStreamPublisher jetStream;
    private final MapClient maps;
    private final String mapId;
    private final String worldId;

    public JetStreamWalSink(JetStreamPublisher jetStream, MapClient maps, String mapId, String worldId) {
        this.jetStream = jetStream;
        this.maps = maps;
        this.mapId = mapId;
        this.worldId = worldId;
    }

    @Override
    public void accept(WalWriter.Batch batch) {
        for (int attempt = 0; ; attempt++) {
            try {
                publish(batch);
                return;
            } catch (RuntimeException e) {
                if (attempt == RETRY_DELAYS_MILLIS.length) throw e;
            }
            try {
                Thread.sleep(RETRY_DELAYS_MILLIS[attempt]);
            } catch (InterruptedException e) {
                HttpClientWrapper.sneakyThrow(e);
            }
        }
    }

    private void publish(WalWriter.Batch batch) {
        var headers = new Headers();
        // The stream drops a second message with the same id, which is what makes a retry after a lost ack safe.
        headers.put(HEADER_MSG_ID, mapId + ":" + batch.firstSeq());
        headers.put(HEADER_WORLD, worldId);
        headers.put(HEADER_FIRST_SEQ, Long.toString(batch.firstSeq()));
        headers.put(HEADER_LAST_SEQ, Long.toString(batch.lastSeq()));
        headers.put(HEADER_FIRST_AT, Long.toString(batch.firstAt()));
        headers.put(HEADER_LAST_AT, Long.toString(batch.lastAt()));
        headers.put(HEADER_LENGTH, Integer.toString(batch.bytes().length));

        byte[] body = batch.bytes();
        if (body.length > MAX_INLINE_BYTES) {
            headers.put(HEADER_OBJECT, maps.putWalBlob(mapId, worldId, body));
            body = EMPTY;
        }
        jetStream.publish("map-wal." + mapId, headers, body);
    }
}
