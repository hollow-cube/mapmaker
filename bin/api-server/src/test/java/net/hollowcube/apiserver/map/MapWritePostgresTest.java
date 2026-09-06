package net.hollowcube.apiserver.map;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.apiserver.s3.MemoryS3Client;
import net.hollowcube.apiserver.s3.S3Client;
import net.hollowcube.ipc.Blob;
import net.hollowcube.ipc.map.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/// The races: two creates for the last slot, two publishes of one map, a publish against an upload
/// that holds the row lock. `TestDb` hands out one connection, so there is no second session to
/// race with; this needs a real Postgres, which CI provides and a developer names in
/// `MAP_WRITE_TEST_DATABASE_URL`.
@EnabledIfEnvironmentVariable(named = "MAP_WRITE_TEST_DATABASE_URL", matches = ".+")
class MapWritePostgresTest {

    private static final String SCHEMA = "map_write_test_"
        + UUID.randomUUID().toString().replace("-", "");
    private static HikariDataSource pool;
    private ApiDatabase db;
    private MapServiceImpl service;
    private MemoryS3Client storage;
    private FakeMapClients clients;
    private UUID owner;

    @BeforeAll
    static void schema() throws Exception {
        var url = System.getenv("MAP_WRITE_TEST_DATABASE_URL");
        try (var conn = DriverManager.getConnection(url); var st = conn.createStatement()) {
            st.execute("create schema " + SCHEMA);
        }
        var config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setSchema(SCHEMA);
        config.setMaximumPoolSize(16);
        pool = new HikariDataSource(config);
        try (
            var conn = pool.getConnection();
            var st = conn.createStatement();
            var files = Files.list(Path.of("../../modules/api/src/main/sql/migrations"))
        ) {
            for (var file : files.filter(p -> p.toString().endsWith(".sql")).sorted().toList())
                st.execute(Files.readString(file));
            st.execute(Files.readString(Path.of("src/test/resources/map-ratings-trigger.sql")));
        }
    }

    @AfterAll
    static void cleanup() throws Exception {
        if (pool != null) pool.close();
        try (
            var conn = DriverManager.getConnection(System.getenv("MAP_WRITE_TEST_DATABASE_URL"));
            var st = conn.createStatement()
        ) {
            st.execute("drop schema " + SCHEMA + " cascade");
        }
    }

    @BeforeEach
    void start() throws Exception {
        try (var conn = pool.getConnection(); var st = conn.createStatement()) {
            st.execute("truncate maps, player_data cascade");
        }
        db = new ApiDatabase(pool);
        storage = new MemoryS3Client();
        clients = new FakeMapClients();
        service = new MapServiceImpl(
            db,
            storage,
            clients.nats,
            clients.redis,
            clients.posthog,
            Duration.ZERO
        );
        owner = player();
    }

    @AfterEach
    void stop() {
        clients.close();
    }

    @Test
    void create_racesForTheLastSlotHandOutOne() throws Exception {
        create(owner);
        var results = race(12, () -> service.create(owner, MapSize.NORMAL, 776));
        assertEquals(1, results.stream().filter(r -> r instanceof CreateMapResult.Success).count());
        assertEquals(
            11,
            results.stream().filter(r -> r instanceof CreateMapResult.NoSlots).count()
        );
        assertEquals(2, db.maps.countSlots(owner));
    }

    @Test
    void acceptBuilderInvite_sharesTheSlotLockWithCreate() throws Exception {
        var builder = player();
        var map = create(owner);
        service.inviteBuilder(map, builder);
        create(builder);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var gate = new CountDownLatch(1);
            var acceptance = executor.submit(() -> {
                gate.await();
                return service.acceptBuilderInvite(map, builder);
            });
            var creation = executor.submit(() -> {
                gate.await();
                return service.create(builder, MapSize.NORMAL, 776);
            });
            gate.countDown();
            var accepted = acceptance.get(10, TimeUnit.SECONDS);
            var created = creation.get(10, TimeUnit.SECONDS);
            assertEquals(
                1,
                (accepted instanceof BuilderResult.Success ? 1 : 0)
                    + (created instanceof CreateMapResult.Success ? 1 : 0)
            );
            assertEquals(2, db.maps.countSlots(builder));
        }
    }

    @Test
    void inviteBuilder_racesCannotExceedCapacity() throws Exception {
        var map = create(owner);
        var targets = new ArrayList<UUID>();
        for (int i = 0; i < 8; i++) targets.add(player());
        var next = new AtomicInteger();
        var results = race(
            8,
            () -> service.inviteBuilder(map, targets.get(next.getAndIncrement()))
        );
        assertEquals(1, results.stream().filter(r -> r instanceof BuilderResult.Success).count());
        assertEquals(2, service.getBuilders(map, false).size());
    }

    @Test
    void setPlayerRating_countsEachRatingOnceThroughTheTrigger() {
        var map = create(owner);
        var rater = player();
        service.setPlayerRating(map, rater, MapRating.LIKED);
        service.setPlayerRating(map, rater, MapRating.LIKED);
        assertEquals(1, service.get(map.toString()).likes());
        service.setPlayerRating(map, rater, MapRating.DISLIKED);
        assertEquals(-1, service.get(map.toString()).likes());
        service.setPlayerRating(map, rater, MapRating.UNRATED);
        assertEquals(0, service.get(map.toString()).likes());
        service.report(map, rater, List.of(MapReportCategory.SPAM), "spam");
        assertEquals(-1, service.get(map.toString()).likes());
    }

    @Test
    void publish_waitsForAnUploadHoldingTheLock() throws Exception {
        var map = create(owner);
        preparePublish(map);
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var blocked = new MapServiceImpl(
            db,
            new BlockingStorage(storage, started, release),
            clients.nats,
            clients.redis,
            clients.posthog,
            Duration.ZERO
        );
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var upload = executor.submit(
                () -> blocked.updateWorld(map, 0, Blob.of(new byte[] {1, 2, 3}))
            );
            assertTrue(started.await(5, TimeUnit.SECONDS));
            var publish = executor.submit(() -> blocked.publish(map));
            try {
                assertThrows(TimeoutException.class, () -> publish.get(200, TimeUnit.MILLISECONDS));
            } finally {
                release.countDown();
            }
            upload.get(5, TimeUnit.SECONDS);
            assertInstanceOf(PublishMapResult.Success.class, publish.get(5, TimeUnit.SECONDS));
            assertArrayEquals(new byte[] {1, 2, 3}, storage.objects().get(map.toString()));
        } finally {
            release.countDown();
        }
    }

    @Test
    void beginVerification_waitsForTheFinalUpload() throws Exception {
        var map = create(owner);
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var blocked = new MapServiceImpl(
            db,
            new BlockingStorage(storage, started, release),
            clients.nats,
            clients.redis,
            clients.posthog,
            Duration.ZERO
        );
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var upload = executor.submit(
                () -> blocked.updateWorld(map, 0, Blob.of(new byte[] {7}))
            );
            assertTrue(started.await(5, TimeUnit.SECONDS));
            var verification = executor.submit(() -> blocked.beginVerification(map));
            try {
                assertThrows(
                    TimeoutException.class,
                    () -> verification.get(200, TimeUnit.MILLISECONDS)
                );
            } finally {
                release.countDown();
            }
            upload.get(5, TimeUnit.SECONDS);
            assertEquals(BeginVerificationResult.READY, verification.get(5, TimeUnit.SECONDS));
            assertEquals(MapVerification.PENDING, service.get(map.toString()).verification());
        } finally {
            release.countDown();
        }
    }

    @Test
    void publish_retriesAnIdCollisionInAFreshTransaction() {
        var first = create(owner);
        preparePublish(first);
        storage.objects().put(first.toString(), new byte[] {1});
        var fixed = new MapServiceImpl(
            db,
            storage,
            clients.nats,
            clients.redis,
            clients.posthog,
            Duration.ZERO,
            Duration.ZERO,
            () -> 123456789
        );
        fixed.publish(first);
        var second = create(owner);
        preparePublish(second);
        storage.objects().put(second.toString(), new byte[] {1});
        var next = new AtomicInteger();
        var retrying = new MapServiceImpl(
            db,
            storage,
            clients.nats,
            clients.redis,
            clients.posthog,
            Duration.ZERO,
            Duration.ZERO,
            () -> next.getAndIncrement() == 0 ? 123456789 : 987654321
        );
        assertEquals(
            "987-654-321",
            assertInstanceOf(PublishMapResult.Success.class, retrying.publish(second)).map()
                .publishedId()
        );
    }

    @Test
    void publish_racesKeepOneId() throws Exception {
        var map = create(owner);
        preparePublish(map);
        storage.objects().put(map.toString(), new byte[] {1});
        var results = race(8, () -> service.publish(map));
        assertEquals(
            1,
            results.stream()
                .map(r -> assertInstanceOf(PublishMapResult.Success.class, r).map().publishedId())
                .distinct()
                .count()
        );
        assertEquals(1, clients.finishAnalytics().stream().filter("map_published"::equals).count());
    }

    private UUID player() {
        var id = UUID.randomUUID();
        sql(
            """
            insert into player_data (id, username, first_join, last_online, online)
            values ('%s', 'player', now(), now(), false)
            """
                .formatted(id)
        );
        return id;
    }

    private UUID create(UUID player) {
        return assertInstanceOf(
            CreateMapResult.Success.class,
            service.create(player, MapSize.NORMAL, 776)
        ).map()
            .id();
    }

    private void preparePublish(UUID map) {
        var patch = new MapPatch.Builder(MapData.draft(map, owner));
        patch.setName("Test");
        patch.setIcon("minecraft:stone");
        patch.setVariant(MapVariant.BUILDING);
        patch.setTags(List.of("terrain"));
        service.update(map, patch.build());
        sql(
            """
            insert into save_states (id, map_id, player_id, type, created, updated, completed, playtime, state_v2)
            values ('%s', '%s', '%s', 'editing', now(), now(), false, 1, '{}')
            """
                .formatted(UUID.randomUUID(), map, owner)
        );
    }

    private void sql(String sql) {
        try (var conn = pool.getConnection(); var st = conn.createStatement()) {
            st.execute(sql);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static <T> List<T> race(int count, Callable<T> call) throws Exception {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var gate = new CountDownLatch(1);
            var futures = new ArrayList<Future<T>>();
            for (int i = 0; i < count; i++)
                futures.add(
                    executor.submit(() -> {
                        gate.await();
                        return call.call();
                    })
                );
            gate.countDown();
            var results = new ArrayList<T>();
            for (var f : futures) results.add(f.get(15, TimeUnit.SECONDS));
            return results;
        }
    }

    private record BlockingStorage(
        S3Client delegate,
        CountDownLatch started,
        CountDownLatch release
    ) implements S3Client {
        @Override
        public void put(String key, InputStream body, long length) {
            started.countDown();
            try {
                if (!release.await(5, TimeUnit.SECONDS))
                    throw new AssertionError("upload was never released");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
            delegate.put(key, body, length);
        }

        @Override
        public Blob get(String key) {
            return delegate.get(key);
        }

        @Override
        public Blob getRange(String key, long a, long b) {
            return delegate.getRange(key, a, b);
        }

        @Override
        public long stat(String key) {
            return delegate.stat(key);
        }

        @Override
        public void delete(String key) {
            delegate.delete(key);
        }

        @Override
        public List<String> list(String prefix) {
            return delegate.list(prefix);
        }
    }
}
