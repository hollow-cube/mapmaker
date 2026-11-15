package net.hollowcube.apiserver.map;

import io.nats.client.Nats;
import io.nats.client.impl.Headers;
import net.hollowcube.apiserver.common.NatsPublisher;
import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.ipc.Wire;
import net.hollowcube.sqlgen.testing.TestDb;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The consumer against a real broker, which is the only thing that can say whether the stream and
/// consumer configuration is one JetStream accepts.
@EnabledIfEnvironmentVariable(named = "MAP_WRITE_TEST_NATS", matches = ".+")
class MapWalConsumerTest {

    @RegisterExtension
    static final TestDb TEST_DB = TestDb.of(
        "../../modules/api/src/main/sql/migrations",
        TestDb.Mode.TRUNCATE
    );

    @Test
    void aPublishedBatchIsStoredAndAcked() throws Exception {
        var db = TEST_DB.database(ApiDatabase::new);
        var mapId = UUID.randomUUID();
        try (
            var nc = Nats.connect(System.getenv("MAP_WRITE_TEST_NATS"));
            var consumer = MapWalConsumer.start(new NatsPublisher(nc, Wire.gson()), db)
        ) {
            var management = nc.jetStreamManagement();
            var deadline = System.nanoTime() + 10_000_000_000L;
            while (!management.getStreamNames().contains(MapWalConsumer.STREAM)) {
                assertTrue(System.nanoTime() < deadline, "the stream was never created");
                Thread.sleep(50);
            }

            var headers = new Headers();
            headers.put("Nats-Msg-Id", mapId + ":1");
            headers.put(MapWalConsumer.WORLD, "world");
            headers.put(MapWalConsumer.FIRST_SEQ, "1");
            headers.put(MapWalConsumer.LAST_SEQ, "2");
            headers.put(MapWalConsumer.FIRST_AT, "1794700000000");
            headers.put(MapWalConsumer.LAST_AT, "1794700000500");
            headers.put(MapWalConsumer.LENGTH, "3");
            var ack = nc.jetStream().publish(
                MapWalConsumer.SUBJECT_PREFIX + mapId,
                headers,
                new byte[] {1, 2, 3}
            );
            assertEquals(MapWalConsumer.STREAM, ack.getStream());

            // Read off the consumer rather than the table: the embedded database has one
            // connection, and the handler is using it.
            while (true) {
                var info = management.getConsumerInfo(MapWalConsumer.STREAM, "map-wal-ingest");
                if (info.getAckFloor().getStreamSequence() >= ack.getSeqno()
                    && info.getNumAckPending() == 0)
                    break;
                assertTrue(System.nanoTime() < deadline, "the batch was never acked");
                Thread.sleep(50);
            }
        }
        try (
            var st = TEST_DB.conn().createStatement();
            var rs = st.executeQuery(
                "select count(*) from map_wal where map_id='" + mapId + "' and length=3"
            )
        ) {
            rs.next();
            assertEquals(1, rs.getLong(1));
        }
    }
}
