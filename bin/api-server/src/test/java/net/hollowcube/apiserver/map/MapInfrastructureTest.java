package net.hollowcube.apiserver.map;

import com.google.gson.JsonObject;
import io.nats.client.Nats;
import io.nats.client.api.StreamConfiguration;
import net.hollowcube.apiserver.common.NatsPublisher;
import net.hollowcube.apiserver.common.Pools;
import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.apiserver.s3.HttpS3Client;
import net.hollowcube.apiserver.s3.S3Client;
import net.hollowcube.ipc.Wire;
import net.hollowcube.ipc.map.*;
import net.hollowcube.posthog.PostHog;
import net.hollowcube.sqlgen.testing.TestDb;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.ByteArrayInputStream;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/// The same calls against real nats, redis and s3, named in `MAP_WRITE_TEST_NATS`,
/// `MAP_WRITE_TEST_REDIS` and `MAP_WRITE_TEST_S3`; everything else fakes them.
@EnabledIfEnvironmentVariable(named = "MAP_WRITE_TEST_NATS", matches = ".+")
class MapInfrastructureTest {

    // TRUNCATE rather than ROLLBACK: every write here goes through `db.txResult`, which cannot
    // commit inside a transaction the harness is holding open.
    @RegisterExtension
    static final TestDb TEST_DB = TestDb.of(
        "../../modules/api/src/main/sql/migrations",
        TestDb.Mode.TRUNCATE
    );

    @Test
    void service_reachesRealNatsRedisAndS3() throws Exception {
        var owner = UUID.randomUUID();
        TEST_DB.seed(
            """
            insert into player_data (id, username, first_join, last_online, online)
            values ('%s', 'owner', now(), now(), false)
            """
                .formatted(owner)
        );
        try (
            var nc = Nats.connect(System.getenv("MAP_WRITE_TEST_NATS"));
            var redis = Pools.redis(System.getenv("MAP_WRITE_TEST_REDIS"))
        ) {
            var management = nc.jetStreamManagement();
            management.addStream(
                StreamConfiguration.builder()
                    .name("MAP_MANAGEMENT")
                    .subjects("map.>")
                    .maxAge(Duration.ofMinutes(5))
                    .build()
            );
            var s3 = new HttpS3Client(
                HttpClient.newHttpClient(),
                System.getenv("MAP_WRITE_TEST_S3"),
                "mapmaker",
                "us-east-1",
                "mapwritetest",
                "mapwritetest"
            );
            s3.createBucketIfAbsent();
            var service = new MapServiceImpl(
                TEST_DB.database(ApiDatabase::new),
                s3,
                new NatsPublisher(nc, Wire.gson()),
                redis,
                PostHog.getClient(),
                Duration.ZERO
            );
            var map = assertInstanceOf(
                CreateMapResult.Success.class,
                service.create(owner, MapSize.NORMAL, 776)
            ).map();
            var id = map.id();
            assertEquals(BeginVerificationResult.READY, service.beginVerification(id));
            var drain = MapCompat.mapEvent(MapCompat.MAP_ACTION_DRAIN, id);
            drain.addProperty("drainReason", "verification");
            nc.flush(Duration.ofSeconds(5));
            var stored = management.getLastMessage("MAP_MANAGEMENT", "map.drain");
            assertEquals("map.drain", stored.getSubject());
            assertEquals(
                drain,
                Wire.gson().fromJson(
                    new String(stored.getData(), StandardCharsets.UTF_8),
                    JsonObject.class
                )
            );

            var patch = new MapPatch.Builder(map);
            patch.setName("Updated");
            service.update(id, patch.build());
            redis.zadd("map:" + id + ":lb_playtime", 100, "player");
            assertEquals(DeleteVerificationResult.RESET, service.deleteVerification(id));
            assertFalse(redis.exists("map:" + id + ":lb_playtime"));

            var bytes = new byte[] {0, 1, 2, 3, 4};
            s3.put(id.toString(), new ByteArrayInputStream(bytes), bytes.length);
            assertEquals(bytes.length, s3.stat(id.toString()));
            assertArrayEquals(bytes, s3.get(id.toString()).readAllBytes());
            s3.delete(id.toString());
            assertThrows(S3Client.NotFoundError.class, () -> s3.stat(id.toString()));
        }
    }
}
