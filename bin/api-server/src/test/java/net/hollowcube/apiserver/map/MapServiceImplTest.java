package net.hollowcube.apiserver.map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/// Every write through the ipc client against the embedded database, with fakes standing in for
/// nats, redis and posthog so that what they were told can be asserted.
class MapServiceImplTest {

    // TRUNCATE rather than ROLLBACK: every write here goes through `db.txResult`, which cannot
    // commit inside a transaction the harness is holding open.
    @RegisterExtension
    static final TestDb TEST_DB = TestDb.of(
        "../../modules/api/src/main/sql/migrations",
        TestDb.Mode.TRUNCATE
    );
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BUILDER = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private ApiDatabase db;
    private MemoryS3Client storage;
    private FakeMapClients clients;
    private MapServiceImpl service;
    private MapClient client;
    private HttpServer http;
    private long publishedIdCounter = 1;

    @BeforeEach
    void start() throws Exception {
        db = TEST_DB.database(ApiDatabase::new);
        storage = new MemoryS3Client();
        clients = new FakeMapClients();
        service = new MapServiceImpl(
            db,
            storage,
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
    void delete_succeedsWhenPublishingFailsAfterCommit() {
        var map = create(OWNER);
        clients.publishFailure = new IllegalStateException("broker unavailable");

        client.delete(OWNER, map.id(), null);

        assertNull(client.get(map.id().toString()));
        assertNotNull(db.maps.getMapIncludingDeleted(map.id()).deletedAt());
        assertDoesNotThrow(() -> client.delete(OWNER, map.id(), null));
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
            update player_data set hypercube_end = now() + interval '1 day' where id = '%s'
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
    }

    @Test
    void leaderboard_readsAndWritesGosLowerCaseFormat() {
        var id = create(OWNER).id();
        TEST_DB.seed(
            """
            update maps set leaderboard = '{"asc":false,"format":"percent","score":"q.score"}'
            where id = '%s'
            """
                .formatted(id)
        );
        var read = client.get(id.toString()).settings().leaderboard();
        assertEquals(MapLeaderboard.Format.PERCENT, read.format());
        assertFalse(read.asc());
        assertEquals("q.score", read.score());

        var patch = new MapPatch.Builder(MapData.draft(id, OWNER));
        patch.setLeaderboard(read.withFormat(MapLeaderboard.Format.NUMBER));
        client.update(id, patch.build());
        assertEquals(
            "number",
            column("select leaderboard ->> 'format' from maps where id = '" + id + "'")
        );
        assertEquals(
            MapLeaderboard.Format.NUMBER,
            client.get(id.toString()).settings().leaderboard().format()
        );
    }

    @Test
    void update_refusesTheAdventureVariant() {
        var id = create(OWNER).id();
        assertEquals(
            400,
            assertThrows(
                IpcException.class,
                () -> client.update(id, patch(null, MapVariant.ADVENTURE, null, null))
            ).status()
        );
        assertEquals(MapVariant.PARKOUR, client.get(id.toString()).settings().variant());
    }

    @Test
    void update_replacesTagsInOrderAndRollsBackAnUnknownTag() {
        var id = create(OWNER).id();
        client.update(id, patch("Original", null, List.of("speedrun", "terrain"), null));
        assertEquals(List.of("speedrun", "terrain"), client.get(id.toString()).settings().tags());
        assertThrows(
            IpcException.class,
            () -> client.update(id, patch("Changed", null, List.of("bad-tag"), null))
        );
        assertEquals("Original", client.get(id.toString()).settings().name());
        assertThrows(
            IpcException.class,
            () -> client.update(id, patch(null, null, List.of("terrain", "terrain"), null))
        );
        client.update(id, patch(null, null, List.of(), null));
        assertEquals(List.of(), client.get(id.toString()).settings().tags());
    }

    @Test
    void update_toParkourDeletesOnlyIncompleteRuns() {
        var id = create(OWNER).id();
        TEST_DB.seed(
            """
            update maps set opt_variant='building', published_id=5, published_at=now() where id='%s'
            """
                .formatted(id)
        );
        state(id, OWNER, "editing", false, 99);
        state(id, OWNER, "playing", true, 15);
        state(id, BUILDER, "playing", false, 30);
        state(id, BUILDER, "verifying", false, 30);
        client.update(id, patch(null, MapVariant.PARKOUR, null, null));
        assertEquals(2, count("select count(*) from save_states where deleted is null"));
        assertEquals(
            1,
            count("select count(*) from save_states where completed and deleted is null")
        );
        assertEquals(99L, db.maps.getLatestEditingTime(id, OWNER));
        // A repeated patch is not a second cleanup of newly started runs.
        state(id, BUILDER, "playing", false, 30);
        client.update(id, patch(null, MapVariant.PARKOUR, null, null));
        assertEquals(3, count("select count(*) from save_states where deleted is null"));
    }

    @Test
    void publish_keepsItsIdOnRetry() {
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
        preparePublish(id);
        var contest = UUID.randomUUID();
        TEST_DB.seed("update maps set contest='" + contest + "' where id='" + id + "'");
        assertInstanceOf(MapStatus.ReadyToPublish.class, client.getStatus(id));
        var first = assertInstanceOf(PublishMapResult.Success.class, client.publish(id)).map();
        var second = assertInstanceOf(PublishMapResult.Success.class, client.publish(id)).map();
        assertEquals(first.publishedId(), second.publishedId());
        assertEquals(first.publishedAt(), second.publishedAt());
        assertEquals(contest, second.contest());
        assertEquals(0, db.maps.countSlots(OWNER));
        assertEquals(1, count("select count(*) from jobs where job='index-map'"));
        assertEquals(
            1,
            clients.finishAnalytics().stream().filter(e -> e.equals("map_published")).count()
        );
        assertEquals(id, client.get("123-456-789").id());
    }

    @Test
    void getStatus_followsDraftVerifyingPublishedAndDeleted() {
        assertInstanceOf(MapStatus.NotFound.class, client.getStatus(UUID.randomUUID()));
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
        assertEquals(DeleteVerificationResult.RESET, client.deleteVerification(id));
        assertInstanceOf(MapStatus.Draft.class, client.getStatus(id));
        preparePublish(id);
        assertInstanceOf(MapStatus.ReadyToPublish.class, client.getStatus(id));
        var map = assertInstanceOf(PublishMapResult.Success.class, client.publish(id)).map();
        var published = assertInstanceOf(MapStatus.Published.class, client.getStatus(id));
        assertEquals(map.publishedId(), published.publishedId());
        assertEquals(map.publishedAt(), published.publishedAt());
        client.delete(OWNER, id, "test");
        assertInstanceOf(MapStatus.NotFound.class, client.getStatus(id));
    }

    @Test
    void get_formatsPublishedIdsAndRatesDifficulty() {
        var id = create(OWNER).id();
        assertNull(client.get(id.toString()).publishedId());
        assertEquals(MapDifficulty.UNRATED, client.get(id.toString()).difficulty());
        preparePublish(id);
        var smallId = new MapServiceImpl(
            db,
            storage,
            clients.nats,
            clients.redis,
            clients.posthog,
            Duration.ZERO,
            Duration.ZERO,
            () -> 7
        );
        assertEquals(
            "000-000-007",
            assertInstanceOf(PublishMapResult.Success.class, smallId.publish(id)).map()
                .publishedId()
        );
        assertEquals(id, client.get("000-000-007").id());
        TEST_DB.seed(
            """
            update maps set opt_variant='parkour' where id='%s';
            insert into map_stats(map_id,play_count,win_count) values ('%s',100,90)
            """
                .formatted(id, id)
        );
        var wins = List.of(90, 60, 30, 10, 1);
        var expected = List.of(
            MapDifficulty.EASY, MapDifficulty.MEDIUM, MapDifficulty.HARD,
            MapDifficulty.EXPERT, MapDifficulty.NIGHTMARE
        );
        for (int i = 0; i < wins.size(); i++) {
            TEST_DB.seed(
                "update map_stats set win_count=" + wins.get(i) + " where map_id='" + id + "'"
            );
            assertEquals(expected.get(i), client.get(id.toString()).difficulty());
        }
        TEST_DB.seed("update map_stats set play_count=9 where map_id='" + id + "'");
        assertEquals(MapDifficulty.UNRATED, client.get(id.toString()).difficulty());
        TEST_DB.seed(
            """
            update map_stats set play_count=100 where map_id='%s';
            update maps set opt_variant='building' where id='%s'
            """
                .formatted(id, id)
        );
        assertEquals(MapDifficulty.UNRATED, client.get(id.toString()).difficulty());
    }

    @Test
    void verification_answersOnlyItsOwnOutcomes() {
        var absent = UUID.randomUUID();
        assertEquals(BeginVerificationResult.MAP_NOT_FOUND, client.beginVerification(absent));
        assertEquals(DeleteVerificationResult.MAP_NOT_FOUND, client.deleteVerification(absent));
        var id = create(OWNER).id();
        TEST_DB.seed("update maps set verification=2 where id='" + id + "'");
        assertEquals(BeginVerificationResult.READY, client.beginVerification(id));
        assertEquals(DeleteVerificationResult.RESET, client.deleteVerification(id));
        preparePublish(id);
        assertEquals(BeginVerificationResult.NOT_VERIFIABLE, client.beginVerification(id));
        client.publish(id);
        assertEquals(BeginVerificationResult.MAP_PUBLISHED, client.beginVerification(id));
        assertEquals(DeleteVerificationResult.MAP_PUBLISHED, client.deleteVerification(id));
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
    void updateWorld_refusesPendingAndStaleWorlds() throws Exception {
        var id = create(OWNER).id();
        client.updateWorld(id, Long.MAX_VALUE, Blob.of(new byte[] {1, 2}));
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
    void deleteVerification_deletesOnlyVerifyingStatesAndRepeats() {
        var id = create(OWNER).id();
        state(id, OWNER, "editing", false, 20);
        state(id, OWNER, "verifying", true, 20);
        state(id, BUILDER, "playing", true, 20);
        assertEquals(DeleteVerificationResult.RESET, client.deleteVerification(id));
        assertEquals(DeleteVerificationResult.RESET, client.deleteVerification(id));
        assertEquals(
            List.of("map:" + id + ":lb_playtime", "map:" + id + ":lb_playtime"),
            clients.redis.deleted
        );
        assertEquals(2, count("select count(*) from save_states where deleted is null"));
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
    void publish_retractsInvites() {
        var id = create(OWNER).id();
        client.inviteBuilder(id, BUILDER);
        preparePublish(id);
        client.publish(id);
        assertEquals(
            0,
            count("select count(*) from player_notifications where deleted_at is null")
        );
        assertInstanceOf(BuilderResult.MapPublished.class, client.acceptBuilderInvite(id, BUILDER));
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
    void delete_requiresAReasonAndKeepsTheWorld() {
        var id = create(OWNER).id();
        client.inviteBuilder(id, BUILDER);
        state(id, OWNER, "editing", false, 1);
        storage.objects().put(id.toString(), new byte[] {9});
        assertEquals(
            400,
            assertThrows(IpcException.class, () -> client.delete(BUILDER, id, null)).status()
        );
        assertNotNull(client.get(id.toString()));
        client.delete(OWNER, id, null);
        client.delete(OWNER, id, null);
        assertNull(client.get(id.toString()));
        assertEquals(0, db.maps.countSlots(OWNER));
        assertEquals(0, count("select count(*) from save_states where deleted is null"));
        assertEquals(
            0,
            count("select count(*) from player_notifications where deleted_at is null")
        );
        assertTrue(storage.objects().containsKey(id.toString()));
        assertEquals("user_deletion", db.maps.getMapIncludingDeleted(id).deletedReason());
    }

    /// Go reads `select maps.*` by position, so the mirror has to append its later columns after
    /// the soft-delete ones exactly as the migrations did, not slot them where they read best.
    @Test
    void migration_ordersMapsColumnsAsProductionHasThem() throws Exception {
        var sql = """
            select a.attname from pg_attribute a
            join pg_class c on c.oid = a.attrelid
            join pg_namespace n on n.oid = c.relnamespace
            where n.nspname = 'public' and c.relname = 'maps' and a.attnum > 0 and not a.attisdropped
            order by a.attnum
            """;
        try (var st = TEST_DB.conn().createStatement(); var rows = st.executeQuery(sql)) {
            var columns = new ArrayList<String>();
            while (rows.next()) columns.add(rows.getString(1));
            assertTrue(columns.indexOf("deleted_reason") < columns.indexOf("protocol_version"));
            assertTrue(columns.indexOf("protocol_version") < columns.indexOf("leaderboard"));
        }
    }

    @Test
    void search_filtersListedPublishedMapsAndOrdersBestThenNewest() {
        var good = published("Good one", MapVariant.PARKOUR, 2, 5, "2026-01-01");
        var best = published("Best one", MapVariant.PARKOUR, 5, 1, "2025-01-01");
        var liked = published("Liked one", MapVariant.BUILDING, 2, 9, "2026-02-01");
        var unlisted = published("Hidden", MapVariant.PARKOUR, 5, 0, "2026-03-01");
        TEST_DB.seed("update maps set listed = false where id = '" + unlisted + "'");
        create(OWNER); // unpublished, must not be listed

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
    void search_ratesDifficultyLikeTheGoView() {
        var easy = published("Easy", MapVariant.PARKOUR, 0, 0, "2026-01-01");
        var hard = published("Hard", MapVariant.PARKOUR, 0, 0, "2026-01-02");
        var fresh = published("Fresh", MapVariant.PARKOUR, 0, 0, "2026-01-03");
        TEST_DB.seed(
            """
            insert into map_stats (map_id, play_count, win_count)
            values ('%s', 100, 90), ('%s', 100, 30), ('%s', 5, 5)
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
        var first = client.search(MapSearch.builder().difficulties(MapDifficulty.EASY).build())
            .first();
        assertEquals(100, first.uniquePlays());
        assertEquals(MapDifficulty.EASY, first.difficulty());
        assertEquals(List.of("terrain"), first.settings().tags());
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
            "update save_states set updated = now() - interval '1 hour' where map_id = '"
                + going
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
        assertEquals(List.of(), client.progress(BUILDER, List.of()));
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
        TEST_DB.seed("update save_states set updated = now() - interval '1 hour'");
        state(second, BUILDER, "playing", true, 1);
        state(first, BUILDER, "playing", false, 1);
        client.delete(OWNER, gone, null);

        var history = client.history(BUILDER, 0, 10);
        assertEquals(2, history.count());
        assertEquals(List.of(first, second), ids(history));
        assertEquals(List.of(second), ids(client.history(BUILDER, 1, 1)));
        assertEquals(List.of(), ids(client.history(OWNER, 0, 10)));
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
            update maps set published_id = %s, published_at = '%s', quality_override = %s, total_likes = %s
            where id = '%s'
            """
                .formatted(publishedIdCounter++, publishedAt, quality, likes, id)
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
            insert into save_states (id, map_id, player_id, type, created, updated, completed, playtime, state_v2)
            values ('%s', '%s', '%s', '%s', now(), now(), %s, %s, '{}')
            """
                .formatted(UUID.randomUUID(), map, player, type, completed, playtime)
        );
    }

    private String column(String sql) {
        try (var st = TEST_DB.conn().createStatement(); var rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getString(1);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
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
