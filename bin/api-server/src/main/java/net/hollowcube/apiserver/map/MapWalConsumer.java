package net.hollowcube.apiserver.map;

import io.nats.client.Message;
import io.nats.client.api.*;
import net.hollowcube.apiserver.common.JetStreamConsumer;
import net.hollowcube.apiserver.common.NatsPublisher;
import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.apiserver.db.MapWalQueries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

public final class MapWalConsumer extends JetStreamConsumer {

    public static final String STREAM = "MAP_WAL";
    public static final String SUBJECT_PREFIX = "map-wal.";
    public static final String WORLD = "Hc-Wal-World";
    public static final String FIRST_SEQ = "Hc-Wal-First-Seq";
    public static final String LAST_SEQ = "Hc-Wal-Last-Seq";
    public static final String FIRST_AT = "Hc-Wal-First-At";
    public static final String LAST_AT = "Hc-Wal-Last-At";
    public static final String LENGTH = "Hc-Wal-Length";
    public static final String OBJECT = "Hc-Wal-Object";

    enum Outcome {
        STORED,
        DUPLICATE,
        NOT_HELD,
        MALFORMED,
    }

    private static final Logger logger = LoggerFactory.getLogger(MapWalConsumer.class);

    private final ApiDatabase db;

    public static MapWalConsumer start(NatsPublisher nats, ApiDatabase db) {
        var consumer = new MapWalConsumer(nats, db);
        consumer.start();
        return consumer;
    }

    MapWalConsumer(NatsPublisher nats, ApiDatabase db) {
        super(
            nats,
            StreamConfiguration.builder()
                .name(STREAM)
                .subjects(SUBJECT_PREFIX + ">")
                .storageType(StorageType.File)
                .retentionPolicy(RetentionPolicy.Limits)
                .maxAge(Duration.ofHours(48))
                .duplicateWindow(Duration.ofMinutes(2)),
            ConsumerConfiguration.builder()
                .durable("map-wal-ingest")
                .filterSubject(SUBJECT_PREFIX + ">")
                .ackPolicy(AckPolicy.Explicit)
                .ackWait(Duration.ofSeconds(30))
                .build()
        );
        this.db = db;
    }

    @Override
    protected void handle(Message message) {
        if (ingest(message) == Outcome.NOT_HELD)
            logger.warn(
                "dropped a batch on {} from a world that lost the map",
                message.getSubject()
            );
    }

    /// Throws only when the database does, which is the one case worth delivering again.
    Outcome ingest(Message message) {
        MapWalQueries.InsertBatchParams batch;
        try {
            batch = parse(message);
        } catch (IllegalArgumentException e) {
            logger.error(
                "dropping a malformed write-ahead-log batch on {}: {}",
                message.getSubject(),
                e.getMessage()
            );
            return Outcome.MALFORMED;
        }

        return db.txResult(tx -> {
            // A world that has let go of the map still has its last batches on the way here, so
            // only a different holder turns one away.
            var holder = tx.worldVersions.getEditingWorld(batch.mapId());
            if (holder != null && !holder.id().equals(batch.worldId())) return Outcome.NOT_HELD;
            return tx.mapWal.insertBatch(batch) == 0 ? Outcome.DUPLICATE : Outcome.STORED;
        });
    }

    private static MapWalQueries.InsertBatchParams parse(Message message) {
        var subject = message.getSubject();
        if (!subject.startsWith(SUBJECT_PREFIX)) throw new IllegalArgumentException("subject");
        var mapId = UUID.fromString(subject.substring(SUBJECT_PREFIX.length()));
        var worldId = requiredHeader(message, WORLD);
        var firstSeq = longHeader(message, FIRST_SEQ);
        var lastSeq = longHeader(message, LAST_SEQ);
        if (firstSeq < 1 || lastSeq < firstSeq)
            throw new IllegalArgumentException("sequence range " + firstSeq + " to " + lastSeq);
        var firstAt = Instant.ofEpochMilli(longHeader(message, FIRST_AT));
        var lastAt = Instant.ofEpochMilli(longHeader(message, LAST_AT));
        var length = Math.toIntExact(longHeader(message, LENGTH));

        byte[] data = message.getData();
        byte[] sha256 = null;
        var object = header(message, OBJECT);
        if (object == null) {
            if (data.length == 0 || data.length != length)
                throw new IllegalArgumentException(
                    "announced " + length + " bytes but carried " + data.length
                );
        } else {
            if (data.length != 0)
                throw new IllegalArgumentException("names an object and carries a batch");
            sha256 = HexFormat.of().parseHex(object);
            if (sha256.length != 32 || length < 1)
                throw new IllegalArgumentException("object " + object + " of " + length + " bytes");
            data = null;
        }
        return new MapWalQueries.InsertBatchParams(
            mapId,
            firstSeq,
            lastSeq,
            worldId,
            firstAt,
            lastAt,
            length,
            data,
            sha256
        );
    }
}
