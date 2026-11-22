package net.hollowcube.apiserver.map;

import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import io.nats.client.Message;
import io.nats.client.impl.Headers;
import io.nats.client.impl.NatsMessage;
import net.hollowcube.apiserver.common.Digest;
import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.apiserver.s3.MemoryS3Client;
import net.hollowcube.ipc.Blob;
import net.hollowcube.ipc.PaginatedList;
import net.hollowcube.ipc.map.*;
import net.hollowcube.ipc.util.IpcException;
import net.hollowcube.sqlgen.testing.TestDb;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/// Every write through the ipc client against the embedded database, with fakes standing in for
/// nats, redis and posthog so that what they were told can be asserted.
class MapServiceImplTest {

    @RegisterExtension
    static final TestDb TEST_DB = TestDb.of("../../modules/api/src/main/sql/migrations");
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BUILDER = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private ApiDatabase db;
    private MemoryS3Client storage;
    private MemoryS3Client worldStore;
    private FakeMapClients clients;
    private MapServiceImpl service;
    private MapClient client;
    private HttpServer http;
    private long publishedIdCounter = 1;

    @BeforeEach
    void start() throws Exception {
        db = TEST_DB.database(ApiDatabase::new);
        storage = new MemoryS3Client();
        worldStore = new MemoryS3Client();
        clients = new FakeMapClients();
        service = new MapServiceImpl(
            db,
            storage,
            worldStore,
            clients.nats,
            clients.redis,
            clients.posthog,
            Duration.ofMinutes(30),
            Duration.ZERO,
            () -> 123456789
        );
        http = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        http.createContext(MapServer.PATH, new MapServer(service));
        http.start();
        client = new MapClient(
            HttpClient.newHttpClient(),
            "http://127.0.0.1:" + http.getAddress().getPort()
        );
        TEST_DB.seed(
            """
            insert into player_data (id, username, first_join, last_online, online)
            values ('%s', 'owner', now(), now(), false), ('%s', 'builder', now(), now(), false)
            """
                .formatted(OWNER, BUILDER)
        );
    }

    @AfterEach
    void stop() {
        http.stop(0);
        clients.close();
    }

    @Test
    void create_stopsAtTheSlotLimit() {
        create(OWNER);
        create(OWNER);
        assertInstanceOf(CreateMapResult.NoSlots.class, client.create(OWNER, MapSize.NORMAL, 776));
        assertEquals(2, db.maps.countSlots(OWNER));
        assertEquals(2, clients.finishAnalytics().size());
    }

    @Test
    void create_unlocksMassiveForExtendedLimitsButNeverDisabledSizes() {
        assertInstanceOf(
            CreateMapResult.SizeLocked.class,
            client.create(OWNER, MapSize.MASSIVE, 776)
        );
        TEST_DB.seed(
            """
            update player_data set hypercube_start = now(), hypercube_end = now() + interval '1 day' where id = '%s'
            """
                .formatted(OWNER)
        );
        assertInstanceOf(CreateMapResult.Success.class, client.create(OWNER, MapSize.MASSIVE, 776));
        assertInstanceOf(
            CreateMapResult.SizeLocked.class,
            client.create(OWNER, MapSize.COLOSSAL, 776)
        );
        assertInstanceOf(
            CreateMapResult.SizeLocked.class,
            client.create(OWNER, MapSize.UNLIMITED, 776)
        );
    }

    @Test
    void update_mergesExtraAndClearsDedicatedBooleans() {
        var id = create(OWNER).id();
        client.update(id, patch(null, null, null, "{\"no_jump\":true,\"custom\":{\"a\":1}}"));
        client.update(id, patch(null, null, null, "{\"no_jump\":false,\"second\":2}"));
        var extra = client.get(id.toString()).settings().extra();
        assertFalse(extra.has("no_jump"));
        assertEquals(1, extra.getAsJsonObject("custom").get("a").getAsInt());
        assertEquals(2, extra.get("second").getAsInt());
        assertEquals(
            400,
            assertThrows(
                IpcException.class,
                () -> client.update(id, patch(null, null, null, "{\"no_jump\":\"yes\"}"))
            ).status()
        );
        assertEquals(extra, client.get(id.toString()).settings().extra());

        client.update(id, patch(null, null, null, "{\"custom\":\"a\\u0000b\"}"));
        assertEquals(
            "ab",
            client.get(id.toString()).settings().extra().get("custom").getAsString()
        );
    }

    @Test
    void update_replacesTagsInOrderAndRefusesAnInvalidPatchWhole() {
        var id = create(OWNER).id();
        client.update(id, patch("Original", null, List.of("speedrun", "terrain"), null));
        assertEquals(List.of("speedrun", "terrain"), client.get(id.toString()).settings().tags());

        assertThrows(
            IpcException.class,
            () -> client.update(id, patch("Changed", null, List.of("bad-tag"), null))
        );
        assertThrows(
            IpcException.class,
            () -> client.update(id, patch("Changed", null, List.of("terrain", "terrain"), null))
        );
        assertEquals(
            400,
            assertThrows(
                IpcException.class,
                () -> client.update(id, patch("Changed", MapVariant.ADVENTURE, null, null))
            ).status()
        );
        var unknown = new MapPatch.Builder(MapData.draft(id, OWNER));
        unknown.setName("Changed");
        unknown.setSubVariant("sideways");
        assertEquals(
            400,
            assertThrows(IpcException.class, () -> client.update(id, unknown.build())).status()
        );
        var settings = client.get(id.toString()).settings();
        assertEquals("Original", settings.name());
        assertEquals(MapVariant.PARKOUR, settings.variant());

        client.update(id, patch(null, null, List.of(), null));
        assertEquals(List.of(), client.get(id.toString()).settings().tags());
    }

    @Test
    void update_toParkourDeletesOnlyIncompleteRuns() {
        var id = create(OWNER).id();
        TEST_DB.seed(
            """
            update maps set variant='building', published_id=5, published_at=now() where id='%s'
            """
                .formatted(id)
        );
        state(id, OWNER, "editing", false, 99);
        state(id, OWNER, "playing", true, 15);
        state(id, BUILDER, "playing", false, 30);
        state(id, BUILDER, "verifying", false, 30);
        client.update(id, patch(null, MapVariant.PARKOUR, null, null));
        assertEquals(2, count("select count(*) from save_states where deleted_at is null"));
        assertEquals(
            1,
            count("select count(*) from save_states where completed and deleted_at is null")
        );
        assertEquals(99L, db.maps.getLatestEditingTime(id, OWNER));
        // A repeated patch is not a second cleanup of newly started runs.
        state(id, BUILDER, "playing", false, 30);
        client.update(id, patch(null, MapVariant.PARKOUR, null, null));
        assertEquals(3, count("select count(*) from save_states where deleted_at is null"));
    }

    @Test
    void publish_keepsItsIdOnRetryAndReleasesTheWorldAndInvites() {
        var id = create(OWNER).id();
        var blocked = assertInstanceOf(PublishMapResult.Blocked.class, client.publish(id));
        assertTrue(
            blocked.readiness()
                .missing()
                .containsAll(
                    List.of(
                        PublishRequirement.WORLD,
                        PublishRequirement.NAME,
                        PublishRequirement.ICON,
                        PublishRequirement.TAGS,
                        PublishRequirement.BUILD_TIME,
                        PublishRequirement.VERIFICATION
                    )
                )
        );
        client.inviteBuilder(id, BUILDER);
        open(id, "world");
        client.saveWorld(id, "world", 0, Blob.of(new byte[] {1}));
        preparePublish(id);
        var contest = UUID.randomUUID();
        TEST_DB.seed("update maps set contest='" + contest + "' where id='" + id + "'");
        assertInstanceOf(MapStatus.ReadyToPublish.class, client.getStatus(id));

        var first = assertInstanceOf(PublishMapResult.Success.class, client.publish(id)).map();
        var second = assertInstanceOf(PublishMapResult.Success.class, client.publish(id)).map();
        assertEquals(first.publishedId(), second.publishedId());
        assertEquals(first.publishedAt(), second.publishedAt());
        assertEquals(contest, second.contest());
        assertEquals(id, client.get("123-456-789").id());
        assertEquals(0, db.maps.countSlots(OWNER));
        assertEquals(1, count("select count(*) from jobs where job='index-map'"));

        assertInstanceOf(
            SaveWorldResult.NotHeld.class,
            client.saveWorld(id, "world", 0, Blob.of(new byte[] {2}))
        );
        assertEquals(0, count("select count(*) from map_worlds"));
        assertEquals(
            0,
            count("select count(*) from player_notifications where deleted_at is null")
        );
        assertInstanceOf(BuilderResult.MapPublished.class, client.acceptBuilderInvite(id, BUILDER));
        assertEquals(
            1,
            clients.finishAnalytics().stream().filter(e -> e.equals("map_published")).count()
        );
    }

    @Test
    void verification_walksAMapFromDraftThroughPublishToDeleted() {
        var id = create(OWNER).id();
        assertInstanceOf(MapStatus.Draft.class, client.getStatus(id));
        storage.objects().put(id.toString(), new byte[] {1});
        assertEquals(BeginVerificationResult.READY, client.beginVerification(id));
        var verifying = assertInstanceOf(MapStatus.Verifying.class, client.getStatus(id));
        assertTrue(verifying.readiness().missing().contains(PublishRequirement.VERIFICATION));
        assertEquals(
            verifying.readiness(),
            assertInstanceOf(PublishMapResult.Blocked.class, client.publish(id)).readiness()
        );

        state(id, BUILDER, "editing", false, 20);
        state(id, OWNER, "verifying", true, 20);
        state(id, BUILDER, "playing", true, 20);
        assertEquals(DeleteVerificationResult.RESET, client.deleteVerification(id));
        assertEquals(List.of("map:" + id + ":lb_playtime"), clients.redis.deleted);
        assertEquals(2, count("select count(*) from save_states where deleted_at is null"));
        assertInstanceOf(MapStatus.Draft.class, client.getStatus(id));

        preparePublish(id);
        client.update(id, patch(null, MapVariant.FREEFORM, null, null));
        assertEquals(BeginVerificationResult.NOT_VERIFIABLE, client.beginVerification(id));
        assertInstanceOf(MapStatus.ReadyToPublish.class, client.getStatus(id));
        var map = assertInstanceOf(PublishMapResult.Success.class, client.publish(id)).map();
        var published = assertInstanceOf(MapStatus.Published.class, client.getStatus(id));
        assertEquals(map.publishedId(), published.publishedId());
        assertEquals(map.publishedAt(), published.publishedAt());
        assertEquals(BeginVerificationResult.MAP_PUBLISHED, client.beginVerification(id));
        assertEquals(DeleteVerificationResult.MAP_PUBLISHED, client.deleteVerification(id));

        client.delete(OWNER, id, "test");
        assertInstanceOf(MapStatus.NotFound.class, client.getStatus(id));
    }

    @Test
    void get_formatsPublishedIdsAndRatesDifficulty() {
        var id = create(OWNER).id();
        preparePublish(id);
        var smallId = new MapServiceImpl(
            db,
            storage,
            worldStore,
            clients.nats,
            clients.redis,
            clients.posthog,
            Duration.ZERO,
            Duration.ZERO,
            () -> 7
        );
        assertEquals(
            "000-000-007",
            assertInstanceOf(PublishMapResult.Success.class, smallId.publish(id))
                .map()
                .publishedId()
        );
        assertEquals(id, client.get("000-000-007").id());
        TEST_DB.seed(
            """
            update maps set variant='parkour' where id='%s';
            insert into map_stats(map_id,play_count,win_count) values ('%s',100,30)
            """
                .formatted(id, id)
        );
        assertEquals(MapDifficulty.HARD, client.get(id.toString()).difficulty());
        TEST_DB.seed("update map_stats set play_count=9 where map_id='" + id + "'");
        assertEquals(MapDifficulty.UNRATED, client.get(id.toString()).difficulty());
    }

    @Test
    void beginVerification_timesOutWithoutChangingTheMap() {
        var id = create(OWNER).id();
        TEST_DB.seed(
            """
            insert into server_states(id,role) values ('editor','map');
            insert into map_worlds(id,map_id,server_id) values ('world','%s','editor')
            """
                .formatted(id)
        );
        assertEquals(BeginVerificationResult.DRAIN_TIMEOUT, client.beginVerification(id));
        assertEquals(MapVerification.UNVERIFIED, client.get(id.toString()).verification());
        client.updateWorld(id, Long.MAX_VALUE, Blob.of(new byte[] {1, 2}));
        TEST_DB.seed("delete from map_worlds");
        assertEquals(BeginVerificationResult.READY, client.beginVerification(id));
        assertEquals(MapVerification.PENDING, client.get(id.toString()).verification());
        assertEquals(BeginVerificationResult.READY, client.beginVerification(id));
        assertEquals(
            2,
            clients.messages.stream().filter(e -> e.subject().equals("map.drain")).count()
        );
    }

    @Test
    void updateWorld_recordsAVersionAndRefusesPendingAndStaleWorlds() throws Exception {
        var id = create(OWNER).id();
        client.updateWorld(id, Long.MAX_VALUE, Blob.of(new byte[] {1, 2}));
        assertEquals(1, count("select count(*) from map_world_versions where world_id is null"));
        client.beginVerification(id);
        assertEquals(
            409,
            assertThrows(
                IpcException.class,
                () -> client.updateWorld(id, Long.MAX_VALUE, Blob.of(new byte[] {3}))
            ).status()
        );
        assertArrayEquals(new byte[] {1, 2}, client.getWorld(id).readAllBytes());
        client.deleteVerification(id);
        preparePublish(id);
        client.publish(id);
        assertEquals(
            409,
            assertThrows(
                IpcException.class,
                () -> client.updateWorld(id, 0, Blob.of(new byte[] {4}))
            ).status()
        );
    }

    @Test
    void saveWorld_storesByDigestOnceAndKeepsTheBareKey() throws Exception {
        var id = create(OWNER).id();
        open(id, "world");
        client.saveWorld(id, "world", 0, Blob.of(new byte[] {1, 2}));
        client.saveWorld(id, "world", 0, Blob.of(new byte[] {1, 2}));

        var key = "worlds/" + Digest.hex(Digest.sha256(new byte[] {1, 2}));
        assertArrayEquals(new byte[] {1, 2}, worldStore.objects().get(key));
        assertArrayEquals(new byte[] {1, 2}, storage.objects().get(id.toString()));
        assertArrayEquals(new byte[] {1, 2}, client.getWorld(id).readAllBytes());
        assertEquals(1, worldStore.puts());
        assertEquals(1, count("select count(*) from map_world_versions where world_id='world'"));
    }

    @Test
    void openWorld_oneWorldHoldsAMapUntilItClosesOrItsServerGoes() {
        var id = create(OWNER).id();
        TEST_DB.seed(
            """
            insert into server_states(id,role) values ('editor','map'), ('other','map');
            insert into map_worlds(id,map_id,server_id,type) values ('first','%s','editor','todo')
            """
                .formatted(id)
        );
        open(id, "first");
        open(id, "first");
        assertEquals(1, count("select count(*) from map_worlds where type='editing'"));
        assertEquals(
            new OpenWorldResult.OpenElsewhere("editor"),
            client.openWorld(id, "second", "other")
        );
        assertInstanceOf(
            SaveWorldResult.NotHeld.class,
            client.saveWorld(id, "second", 0, Blob.of(new byte[] {1}))
        );

        client.saveWorld(id, "first", 0, Blob.of(new byte[] {2}));
        client.closeWorld("first");
        assertInstanceOf(OpenWorldResult.Opened.class, client.openWorld(id, "second", "other"));
        assertInstanceOf(
            SaveWorldResult.NotHeld.class,
            client.saveWorld(id, "first", 0, Blob.of(new byte[] {3}))
        );

        TEST_DB.seed("delete from server_states where id='other'");
        assertInstanceOf(OpenWorldResult.Opened.class, client.openWorld(id, "third", "editor"));
    }

    @Test
    void getWal_answersTheBatchesAfterTheNewestSave() throws Exception {
        var id = create(OWNER).id();
        open(id, "world");
        ingest(id, "world", 1, 2, new byte[] {1});
        client.saveWorld(id, "world", 2, Blob.of(new byte[] {9}));
        ingest(id, "world", 3, 3, new byte[] {7, 8});
        var sha256 = client.putWalBlob(id, "world", Blob.of(new byte[] {4, 5, 6}));
        var headers = headers("world", 4, 6, 3);
        headers.put(MapWalConsumer.OBJECT, sha256);
        new MapWalConsumer(clients.nats, db)
            .ingest(message(MapWalConsumer.SUBJECT_PREFIX + id, headers, new byte[0]));
        ingest(id, "world", 7, 7, new byte[] {2});

        assertEquals(new OpenWorldResult.Opened(7, 2), client.openWorld(id, "world", "editor"));
        try (var wal = client.getWal(id, 2, 6)) {
            assertEquals(13, wal.length());
            assertArrayEquals(
                new byte[] {0, 0, 0, 2, 7, 8, 0, 0, 0, 3, 4, 5, 6},
                wal.readAllBytes()
            );
        }
        try (var wal = client.getWal(id, 7, 7)) {
            assertEquals(0, wal.length());
        }

        client.closeWorld("world");
        client.updateWorld(id, Long.MAX_VALUE, Blob.of(new byte[] {1, 2}));
        assertEquals(new OpenWorldResult.Opened(7, 7), client.openWorld(id, "next", "editor"));
    }

    @Test
    void getWal_readsALogLongerThanOnePage() throws Exception {
        var id = create(OWNER).id();
        open(id, "world");
        for (int seq = 1; seq <= 17; seq++) ingest(id, "world", seq, seq, new byte[] {(byte) seq});
        try (var wal = client.getWal(id, 0, 17)) {
            var bytes = wal.readAllBytes();
            assertEquals(85, wal.length());
            assertEquals(85, bytes.length);
            for (int seq = 1; seq <= 17; seq++) assertEquals(seq, bytes[seq * 5 - 1]);
        }
    }

    @Test
    void ingest_storesEachBatchOfTheHolderOnce() {
        var id = create(OWNER).id();
        open(id, "world");
        assertEquals(MapWalConsumer.Outcome.STORED, ingest(id, "world", 1, 1, new byte[] {1}));
        assertEquals(MapWalConsumer.Outcome.DUPLICATE, ingest(id, "world", 1, 1, new byte[] {1}));
        assertEquals(MapWalConsumer.Outcome.NOT_HELD, ingest(id, "other", 2, 2, new byte[] {1}));
        assertEquals(
            409,
            assertThrows(
                IpcException.class,
                () -> client.putWalBlob(id, "other", Blob.of(new byte[] {1}))
            ).status()
        );
        assertTrue(worldStore.objects().isEmpty());

        var sha256 = client.putWalBlob(id, "world", Blob.of(new byte[] {4, 5, 6}));
        client.putWalBlob(id, "world", Blob.of(new byte[] {4, 5, 6}));
        assertArrayEquals(new byte[] {4, 5, 6}, worldStore.objects().get("wal/" + sha256));
        assertEquals(1, worldStore.puts());
        var headers = headers("world", 2, 3, 3);
        headers.put(MapWalConsumer.OBJECT, sha256);
        assertEquals(
            MapWalConsumer.Outcome.STORED,
            new MapWalConsumer(clients.nats, db)
                .ingest(message(MapWalConsumer.SUBJECT_PREFIX + id, headers, new byte[0]))
        );

        client.closeWorld("world");
        assertEquals(MapWalConsumer.Outcome.STORED, ingest(id, "world", 4, 4, new byte[] {1}));
        assertEquals(3, count("select count(*) from map_wal"));
        assertEquals(
            1,
            count("select count(*) from map_wal where data is null and object_sha256 is not null")
        );
    }

    @Test
    void ingest_dropsAMalformedBatch() {
        var id = create(OWNER).id();
        open(id, "world");
        var subject = MapWalConsumer.SUBJECT_PREFIX + id;
        var noWorld = headers("world", 1, 1, 1);
        noWorld.remove(MapWalConsumer.WORLD);
        var objectAndBody = headers("world", 1, 1, 1);
        objectAndBody.put(MapWalConsumer.OBJECT, "00".repeat(32));
        var notADigest = headers("world", 1, 1, 1);
        notADigest.put(MapWalConsumer.OBJECT, "abcd");
        var body = new byte[] {1};

        var ingest = new MapWalConsumer(clients.nats, db);
        for (var m : List.of(
            message(subject, noWorld, body),
            message(subject, headers("world", 1, 1, 5), body),
            message(subject, headers("world", 3, 2, 1), body),
            message(subject, objectAndBody, body),
            message(subject, notADigest, new byte[0]),
            message(MapWalConsumer.SUBJECT_PREFIX + "not-a-map", headers("world", 1, 1, 1), body),
            message(subject, headers("world", 1, 1, 1), new byte[0])
        ))
            assertEquals(MapWalConsumer.Outcome.MALFORMED, ingest.ingest(m));
        assertEquals(0, count("select count(*) from map_wal"));
    }

    @Test
    void inviteBuilder_answersPreferencesCapacityAndRetries() {
        var id = create(OWNER).id();
        TEST_DB.seed(
            """
            update player_data set settings='{"allow_builder_invites":false}' where id='%s'
            """
                .formatted(BUILDER)
        );
        assertInstanceOf(BuilderResult.InvitesDisabled.class, client.inviteBuilder(id, BUILDER));
        TEST_DB.seed("update player_data set settings='{}' where id='" + BUILDER + "'");
        assertInstanceOf(BuilderResult.Success.class, client.inviteBuilder(id, BUILDER));
        assertInstanceOf(BuilderResult.AlreadyDone.class, client.inviteBuilder(id, BUILDER));
        assertEquals(
            1,
            count("select count(*) from player_notifications where deleted_at is null")
        );
        var other = UUID.randomUUID();
        TEST_DB.seed(
            """
            insert into player_data (id, username, first_join, last_online, online)
            values ('%s', 'other', now(), now(), false)
            """
                .formatted(other)
        );
        assertInstanceOf(BuilderResult.CapacityReached.class, client.inviteBuilder(id, other));
        assertInstanceOf(BuilderResult.Owner.class, client.inviteBuilder(id, OWNER));
    }

    @Test
    void acceptBuilderInvite_takesASlotAndRemovesTheNotification() {
        var id = create(OWNER).id();
        client.inviteBuilder(id, BUILDER);
        var owned = create(BUILDER).id();
        create(BUILDER);
        assertInstanceOf(BuilderResult.NoSlots.class, client.acceptBuilderInvite(id, BUILDER));
        assertEquals(
            1,
            count("select count(*) from player_notifications where deleted_at is null")
        );
        client.delete(BUILDER, owned, null);
        assertInstanceOf(BuilderResult.Success.class, client.acceptBuilderInvite(id, BUILDER));
        assertInstanceOf(BuilderResult.AlreadyDone.class, client.acceptBuilderInvite(id, BUILDER));
        assertEquals(
            0,
            count("select count(*) from player_notifications where deleted_at is null")
        );
        assertEquals(2, client.getBuilders(id, true).size());
        assertEquals(2, db.maps.countSlots(BUILDER));
        assertTrue(
            clients.messages.stream().anyMatch(m -> m.subject().equals("notification.deleted"))
        );
    }

    @Test
    void removeBuilder_repeatsAndNeverRemovesTheOwner() {
        var id = create(OWNER).id();
        client.inviteBuilder(id, BUILDER);
        assertInstanceOf(BuilderResult.Success.class, client.rejectBuilderInvite(id, BUILDER));
        assertInstanceOf(BuilderResult.AlreadyDone.class, client.rejectBuilderInvite(id, BUILDER));
        assertEquals(
            1,
            count("select count(*) from player_notifications where type='map_builder_rejected'")
        );
        assertInstanceOf(BuilderResult.Owner.class, client.removeBuilder(id, OWNER));
        assertEquals(1, db.maps.countSlots(OWNER));
        client.inviteBuilder(id, BUILDER);
        assertInstanceOf(BuilderResult.Success.class, client.removeBuilder(id, BUILDER));
        var transfer = clients.messages.stream()
            .filter(m -> m.subject().equals("player.transfer"))
            .findFirst()
            .orElseThrow()
            .body();
        assertEquals(id.toString(), transfer.get("from").getAsString());
    }

    @Test
    void report_requiresACommentForUnplayableAndDislikes() {
        var id = create(OWNER).id();
        assertThrows(IpcException.class, () -> client.report(id, BUILDER, List.of(), null));
        assertThrows(
            IpcException.class,
            () -> client.report(id, BUILDER, List.of(MapReportCategory.UNPLAYABLE), "")
        );
        client.report(id, BUILDER, List.of(MapReportCategory.CHEATED), null);
        assertEquals(MapRating.UNRATED, client.getPlayerRating(id, BUILDER));
        client.report(id, BUILDER, List.of(MapReportCategory.SPAM), "spam");
        assertEquals(MapRating.DISLIKED, client.getPlayerRating(id, BUILDER));
        assertEquals(2, count("select count(*) from map_reports"));
    }

    @Test
    void setPlayerRating_storesUnratedAsNull_andCountsOnTheNextStatsRefresh() {
        var id = create(OWNER).id();
        var likes = "select likes from map_stats where map_id = '" + id + "'";
        client.setPlayerRating(id, OWNER, MapRating.LIKED);
        client.setPlayerRating(id, BUILDER, MapRating.LIKED);
        db.mapStats.refresh(List.of(id));
        assertEquals(2, count(likes));
        client.setPlayerRating(id, BUILDER, MapRating.DISLIKED);
        assertEquals(MapRating.DISLIKED, client.getPlayerRating(id, BUILDER));
        db.mapStats.refresh(List.of(id));
        assertEquals(0, count(likes));
        client.setPlayerRating(id, BUILDER, MapRating.UNRATED);
        assertEquals(MapRating.UNRATED, client.getPlayerRating(id, BUILDER));
        db.mapStats.refresh(List.of(id));
        assertEquals(1, count(likes));
        assertEquals(1, count("select count(*) from map_ratings where rating is null"));
    }

    @Test
    void delete_requiresAReasonKeepsTheWorldAndSurvivesAFailedPublish() {
        var id = create(OWNER).id();
        client.inviteBuilder(id, BUILDER);
        state(id, OWNER, "editing", false, 1);
        storage.objects().put(id.toString(), new byte[] {9});
        assertEquals(
            400,
            assertThrows(IpcException.class, () -> client.delete(BUILDER, id, null)).status()
        );
        assertNotNull(client.get(id.toString()));

        clients.publishFailure = new IllegalStateException("broker unavailable");
        client.delete(OWNER, id, null);
        client.delete(OWNER, id, null);
        assertNull(client.get(id.toString()));
        assertEquals(0, db.maps.countSlots(OWNER));
        assertEquals(0, count("select count(*) from save_states where deleted_at is null"));
        assertEquals(
            0,
            count("select count(*) from player_notifications where deleted_at is null")
        );
        assertTrue(storage.objects().containsKey(id.toString()));
        assertEquals("user_deletion", db.maps.getMapIncludingDeleted(id).deletedReason());
    }

    @Test
    void search_filtersListedPublishedMapsAndOrdersBestThenNewest() {
        var good = published("Good one", MapVariant.PARKOUR, 2, 5, "2026-01-01");
        var best = published("Best one", MapVariant.PARKOUR, 5, 1, "2025-01-01");
        var liked = published("Liked one", MapVariant.BUILDING, 2, 9, "2026-02-01");
        var unlisted = published("Hidden", MapVariant.PARKOUR, 5, 0, "2026-03-01");
        TEST_DB.seed("update maps set listed = false where id = '" + unlisted + "'");
        create(OWNER);

        var byBest = client.search(MapSearch.builder().sort(MapSearch.Sort.BEST).build());
        assertEquals(3, byBest.count());
        assertEquals(List.of(best, liked, good), ids(byBest));
        assertEquals(
            List.of(liked, good, best),
            ids(client.search(MapSearch.builder().sort(MapSearch.Sort.PUBLISHED).build()))
        );
        assertEquals(
            List.of(best, good, liked),
            ids(
                client.search(
                    MapSearch.builder().sort(MapSearch.Sort.PUBLISHED).ascending(true).build()
                )
            )
        );
        assertEquals(
            List.of(liked),
            ids(client.search(MapSearch.builder().variants(MapVariant.BUILDING).build()))
        );
        assertEquals(
            List.of(best),
            ids(client.search(MapSearch.builder().qualities(MapQuality.MASTERPIECE).build()))
        );
        assertEquals(
            List.of(liked, good),
            ids(client.search(MapSearch.builder().query("d ON").build()))
        );
        assertEquals(List.of(), ids(client.search(MapSearch.builder().owner(BUILDER).build())));
        var page = client.search(
            MapSearch.builder().sort(MapSearch.Sort.BEST).page(1).pageSize(2).build()
        );
        assertEquals(3, page.count());
        assertEquals(List.of(good), ids(page));
        // The filter binds sit before offset and limit: a slip would put "one" in the offset.
        var filtered = client.search(
            MapSearch.builder()
                .query("one")
                .qualities(MapQuality.GREAT, MapQuality.MASTERPIECE)
                .variants(MapVariant.PARKOUR)
                .sort(MapSearch.Sort.BEST)
                .page(1)
                .pageSize(1)
                .build()
        );
        assertEquals(2, filtered.count());
        assertEquals(List.of(good), ids(filtered));
        assertThrows(
            IpcException.class,
            () -> service.search(MapSearch.builder().sort(MapSearch.Sort.UNKNOWN).build())
        );
    }

    @Test
    void search_filtersByDifficulty() {
        var easy = published("Easy", MapVariant.PARKOUR, 0, 0, "2026-01-01");
        var hard = published("Hard", MapVariant.PARKOUR, 0, 0, "2026-01-02");
        var fresh = published("Fresh", MapVariant.PARKOUR, 0, 0, "2026-01-03");
        TEST_DB.seed(
            """
            insert into map_stats (map_id, play_count, win_count)
            values ('%s', 100, 90), ('%s', 100, 30), ('%s', 5, 5)
            on conflict (map_id) do update set play_count = excluded.play_count, win_count = excluded.win_count
            """
                .formatted(easy, hard, fresh)
        );
        assertEquals(
            List.of(easy),
            ids(client.search(MapSearch.builder().difficulties(MapDifficulty.EASY).build()))
        );
        assertEquals(
            List.of(hard),
            ids(client.search(MapSearch.builder().difficulties(MapDifficulty.HARD).build()))
        );
        assertEquals(
            List.of(fresh),
            ids(client.search(MapSearch.builder().difficulties(MapDifficulty.UNRATED).build()))
        );
    }

    @Test
    void progress_reportsTheBestFinishedRunElseTheLatest() {
        TEST_DB.seed("update player_data set extra_map_slots = 10");
        var done = create(OWNER).id();
        var going = create(OWNER).id();
        var never = create(OWNER).id();
        state(done, BUILDER, "playing", true, 30_000);
        state(done, BUILDER, "playing", true, 20_049);
        state(done, BUILDER, "playing", false, 1_000);
        state(going, BUILDER, "verifying", false, 4_000);
        TEST_DB.seed(
            "update save_states set updated_at = now() - interval '1 hour' where map_id = '" + going
                + "'"
        );
        state(going, BUILDER, "playing", false, 7_000);
        state(going, OWNER, "playing", true, 1);
        state(never, BUILDER, "editing", true, 1);

        var progress = new HashMap<UUID, PlayerMapProgress>();
        for (var entry : client.progress(BUILDER, List.of(done, going, never))) {
            progress.put(entry.mapId(), entry);
        }
        assertEquals(2, progress.size());
        assertEquals(PlayerMapProgress.Progress.COMPLETE, progress.get(done).progress());
        assertEquals(20_050, progress.get(done).playtime());
        assertEquals(PlayerMapProgress.Progress.STARTED, progress.get(going).progress());
        assertEquals(7_000, progress.get(going).playtime());
    }

    @Test
    void history_listsMapsLastPlayedFirstWithoutDeletedOnes() {
        TEST_DB.seed("update player_data set extra_map_slots = 10");
        var first = create(OWNER).id();
        var second = create(OWNER).id();
        var gone = create(OWNER).id();
        state(first, BUILDER, "playing", false, 1);
        state(gone, BUILDER, "playing", false, 1);
        state(second, BUILDER, "editing", false, 1);
        TEST_DB.seed("update save_states set updated_at = now() - interval '1 hour'");
        state(second, BUILDER, "playing", true, 1);
        TEST_DB.seed(
            "update save_states set updated_at = now() - interval '1 minute' where updated_at = now()"
        );
        state(first, BUILDER, "playing", false, 1);
        client.delete(OWNER, gone, null);

        var history = client.history(BUILDER, 0, 10);
        assertEquals(2, history.count());
        assertEquals(List.of(first, second), ids(history));
        assertEquals(List.of(second), ids(client.history(BUILDER, 1, 1)));
    }

    private MapWalConsumer.Outcome ingest(
        UUID mapId,
        String worldId,
        long firstSeq,
        long lastSeq,
        byte[] body
    ) {
        return new MapWalConsumer(clients.nats, db)
            .ingest(
                message(
                    MapWalConsumer.SUBJECT_PREFIX + mapId,
                    headers(worldId, firstSeq, lastSeq, body.length),
                    body
                )
            );
    }

    private static Message message(String subject, Map<String, String> values, byte[] body) {
        var headers = new Headers();
        values.forEach(headers::put);
        return NatsMessage.builder().subject(subject).headers(headers).data(body).build();
    }

    private static HashMap<String, String> headers(
        String worldId,
        long firstSeq,
        long lastSeq,
        int length
    ) {
        var headers = new HashMap<String, String>();
        headers.put(MapWalConsumer.WORLD, worldId);
        headers.put(MapWalConsumer.FIRST_SEQ, Long.toString(firstSeq));
        headers.put(MapWalConsumer.LAST_SEQ, Long.toString(lastSeq));
        headers.put(MapWalConsumer.FIRST_AT, "1794700000000");
        headers.put(MapWalConsumer.LAST_AT, "1794700000500");
        headers.put(MapWalConsumer.LENGTH, Integer.toString(length));
        return headers;
    }

    private void open(UUID mapId, String worldId) {
        TEST_DB.seed(
            "insert into server_states(id,role) values ('editor','map') on conflict do nothing"
        );
        assertInstanceOf(OpenWorldResult.Opened.class, client.openWorld(mapId, worldId, "editor"));
    }

    private UUID published(
        String name,
        MapVariant variant,
        int quality,
        int likes,
        String publishedAt
    ) {
        var id = create(OWNER).id();
        var patch = new MapPatch.Builder(MapData.draft(id, OWNER));
        patch.setName(name);
        patch.setVariant(variant);
        patch.setTags(List.of("terrain"));
        client.update(id, patch.build());
        TEST_DB.seed(
            """
            update maps set published_id = %s, published_at = '%s',
                            quality = (enum_range(null::map_quality))[%s + 1]
            where id = '%s';
            insert into map_stats (map_id, likes) values ('%s', %s)
            on conflict (map_id) do update set likes = excluded.likes;
            """
                .formatted(publishedIdCounter++, publishedAt, quality, id, id, likes)
        );
        return id;
    }

    private static List<UUID> ids(PaginatedList<MapData> page) {
        return page.results().stream().map(MapData::id).toList();
    }

    private MapData create(UUID player) {
        return assertInstanceOf(
            CreateMapResult.Success.class,
            client.create(player, MapSize.NORMAL, 776)
        ).map();
    }

    private void preparePublish(UUID id) {
        var patch = new MapPatch.Builder(MapData.draft(id, OWNER));
        patch.setName("A map");
        patch.setIcon("minecraft:stone");
        patch.setVariant(MapVariant.BUILDING);
        patch.setTags(List.of("terrain"));
        client.update(id, patch.build());
        storage.objects().put(id.toString(), "world".getBytes(StandardCharsets.UTF_8));
        state(id, OWNER, "editing", false, 1_800_000);
    }

    private static MapPatch patch(
        @Nullable String name,
        @Nullable MapVariant variant,
        @Nullable List<String> tags,
        @Nullable String extra
    ) {
        var patch = new MapPatch.Builder(MapData.draft(UUID.randomUUID(), OWNER));
        if (name != null) patch.setName(name);
        if (variant != null) patch.setVariant(variant);
        if (tags != null) patch.setTags(tags);
        if (extra != null)
            for (var entry : JsonParser.parseString(extra).getAsJsonObject().entrySet())
                patch.setExtra(entry.getKey(), entry.getValue());
        return patch.build();
    }

    private void state(UUID map, UUID player, String type, boolean completed, long playtime) {
        TEST_DB.seed(
            """
            insert into save_states (id, map_id, player_id, type, created_at, updated_at, completed, playtime, state)
            values ('%s', '%s', '%s', '%s', now(), now(), %s, %s, '{}')
            """
                .formatted(UUID.randomUUID(), map, player, type, completed, playtime)
        );
    }

    private long count(String sql) {
        try (var st = TEST_DB.conn().createStatement(); var rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
