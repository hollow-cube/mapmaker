package net.hollowcube.apiserver.map;

import com.sun.net.httpserver.HttpServer;
import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.apiserver.s3.MemoryS3Client;
import net.hollowcube.ipc.map.GlobalLeaderboard;
import net.hollowcube.ipc.map.LeaderboardData;
import net.hollowcube.ipc.map.MapClient;
import net.hollowcube.ipc.map.MapServer;
import net.hollowcube.ipc.map.PlayerTopTime;
import net.hollowcube.ipc.util.IpcException;
import net.hollowcube.sqlgen.testing.TestDb;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/// The leaderboard calls: boards read off a fake redis, the runs behind them in the embedded
/// database.
class MapLeaderboardTest {

    @RegisterExtension
    static final TestDb TEST_DB = TestDb.of(
        "../../modules/api/src/main/sql/migrations",
        TestDb.Mode.TRUNCATE
    );
    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final UUID CAROL = UUID.fromString("00000000-0000-0000-0000-00000000000c");
    private static final UUID DAVE = UUID.fromString("00000000-0000-0000-0000-00000000000d");
    private final AtomicLong publishedIds = new AtomicLong(100);
    private ApiDatabase db;
    private FakeMapClients clients;
    private MapClient client;
    private HttpServer http;

    @BeforeEach
    void start() throws Exception {
        db = TEST_DB.database(ApiDatabase::new);
        clients = new FakeMapClients();
        http = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        http.createContext(MapServer.PATH, new MapServer(service()));
        http.start();
        client = new MapClient(
            HttpClient.newHttpClient(),
            "http://127.0.0.1:" + http.getAddress().getPort()
        );
        for (var player : List.of(ALICE, BOB, CAROL, DAVE))
            TEST_DB.seed(
                "insert into player_data (id, username, first_join, last_online, online) values ('"
                    + player
                    + "', 'p"
                    + player.toString().charAt(35)
                    + "', now(), now(), false)"
            );
    }

    @AfterEach
    void stop() {
        http.stop(0);
        clients.close();
    }

    private MapServiceImpl service() {
        return new MapServiceImpl(
            db,
            new MemoryS3Client(),
            clients.nats,
            clients.redis,
            clients.posthog,
            Duration.ZERO
        );
    }

    @Test
    void getMapLeaderboard_roundsTimesToTheTickAndSharesRanksOnTies() {
        var map = map("parkour", true, null);
        board(map, ALICE, 1000);
        board(map, BOB, 1020);
        board(map, CAROL, 1049);
        board(map, DAVE, 1075);

        var leaderboard = client.getMapLeaderboard(map, DAVE);
        assertEquals(
            List.of(
                new LeaderboardData.Entry(ALICE, 1000, 1),
                new LeaderboardData.Entry(BOB, 1000, 1),
                new LeaderboardData.Entry(CAROL, 1050, 3),
                new LeaderboardData.Entry(DAVE, 1100, 4)
            ),
            leaderboard.top()
        );
        assertEquals(new LeaderboardData.Entry(DAVE, 1100, 4), leaderboard.player());
    }

    @Test
    void getMapLeaderboard_ranksAPlayerOutsideTheTopByTheSameRule() {
        var map = map("parkour", true, null);
        for (var i = 0; i < 10; i++) board(map, UUID.randomUUID(), 1000 + i * 100);
        board(map, ALICE, 1949);
        board(map, BOB, 1975);

        assertEquals(10, client.getMapLeaderboard(map, null).top().size());
        assertEquals(
            new LeaderboardData.Entry(ALICE, 1950, 11),
            client.getMapLeaderboard(map, ALICE).player()
        );
        assertEquals(
            new LeaderboardData.Entry(BOB, 2000, 12),
            client.getMapLeaderboard(map, BOB).player()
        );
        assertNull(client.getMapLeaderboard(map, CAROL).player());
    }

    @Test
    void getMapLeaderboard_readsADescendingNumberBoardFromTheTopUnrounded() {
        var map = map(
            "parkour",
            true,
            "{\"asc\": false, \"format\": \"number\", \"score\": \"q.coins\"}"
        );
        board(map, ALICE, 7);
        board(map, BOB, 12);
        board(map, CAROL, 12);

        var leaderboard = client.getMapLeaderboard(map, ALICE);
        assertEquals(
            List.of(
                new LeaderboardData.Entry(CAROL, 12, 1),
                new LeaderboardData.Entry(BOB, 12, 1),
                new LeaderboardData.Entry(ALICE, 7, 3)
            ),
            leaderboard.top(),
            "redis orders a tie by member, so the higher uuid comes first off a reversed range"
        );
        assertEquals(new LeaderboardData.Entry(ALICE, 7, 3), leaderboard.player());
    }

    @Test
    void getMapLeaderboard_ranksAFractionalScoreAgainstWhatTheBoardShowsAndNotItsRawScore() {
        var descending = map(
            "parkour",
            true,
            "{\"asc\": false, \"format\": \"number\", \"score\": \"q.coins\"}"
        );
        board(descending, ALICE, 7.5);
        board(descending, BOB, 90.0);
        board(descending, CAROL, 87.5);

        var board = client.getMapLeaderboard(descending, ALICE);
        assertEquals(
            new LeaderboardData.Entry(ALICE, 7, 3),
            board.player(),
            "a score only beats what is shown as a whole number below it, never itself"
        );
        assertEquals(
            List.of(
                new LeaderboardData.Entry(BOB, 90, 1),
                new LeaderboardData.Entry(CAROL, 87, 2),
                new LeaderboardData.Entry(ALICE, 7, 3)
            ),
            board.top()
        );
        assertEquals(
            new LeaderboardData.Entry(CAROL, 87, 2),
            client.getMapLeaderboard(descending, CAROL).player()
        );

        var ascending = map(
            "parkour",
            true,
            "{\"asc\": true, \"format\": \"number\", \"score\": \"q.coins\"}"
        );
        board(ascending, ALICE, 7.5);
        board(ascending, BOB, 7.2);
        board(ascending, CAROL, 6.5);
        assertEquals(
            new LeaderboardData.Entry(ALICE, 7, 2),
            client.getMapLeaderboard(ascending, ALICE).player(),
            "7.2 shows as 7 and ties, 6.5 shows as 6 and does not"
        );
    }

    @Test
    void getMapLeaderboard_isEmptyForABoardNobodyHasFinishedOrAnUnknownMap() {
        assertEquals(
            LeaderboardData.EMPTY,
            client.getMapLeaderboard(map("parkour", true, null), ALICE)
        );
        assertEquals(LeaderboardData.EMPTY, client.getMapLeaderboard(UUID.randomUUID(), null));
    }

    @Test
    void getGlobalLeaderboard_countsBeatenMapsAndFastestTimesOnPublishedBoards() {
        var first = map("parkour", true, null);
        var second = map("parkour", true, null);
        var unpublished = map("parkour", false, null);
        var descending = map(
            "parkour",
            true,
            "{\"asc\": false, \"format\": \"time\", \"score\": \"q.playtime\"}"
        );
        run(first, ALICE, 1000, null);
        run(first, BOB, 1020, null);
        run(first, CAROL, 2000, null);
        run(second, ALICE, 3000, null);
        run(second, BOB, 2000, null);
        run(unpublished, CAROL, 1000, null);
        run(descending, CAROL, 1000, null);

        var mapsBeaten = client.getGlobalLeaderboard(GlobalLeaderboard.MAPS_BEATEN, null);
        assertEquals(
            List.of(
                new LeaderboardData.Entry(ALICE, 2, 1),
                new LeaderboardData.Entry(BOB, 2, 1),
                new LeaderboardData.Entry(CAROL, 2, 1)
            ),
            mapsBeaten.top()
        );
        assertEquals(
            new LeaderboardData.Entry(CAROL, 2, -1),
            client.getGlobalLeaderboard(GlobalLeaderboard.MAPS_BEATEN, CAROL).player()
        );

        var topTimes = client.getGlobalLeaderboard(GlobalLeaderboard.TOP_TIMES, null);
        assertEquals(
            List.of(new LeaderboardData.Entry(BOB, 2, 1), new LeaderboardData.Entry(ALICE, 1, 2)),
            topTimes.top(),
            "a time a tick apart ties for fastest; a descending board counts for nobody"
        );
        assertEquals(
            new LeaderboardData.Entry(CAROL, 0, -1),
            client.getGlobalLeaderboard(GlobalLeaderboard.TOP_TIMES, CAROL).player()
        );
    }

    @Test
    void getPlayerTopTimes_ranksAgainstEachBoardAndPages() {
        var first = map("parkour", true, null);
        var second = map("parkour", true, null);
        var third = map("parkour", true, null);
        run(first, ALICE, 1000, null);
        run(first, ALICE, 3000, null);
        run(first, BOB, 1020, null);
        run(second, ALICE, 2000, null);
        run(second, BOB, 1000, null);
        run(second, CAROL, 1000, null);
        run(third, ALICE, 5000, null);
        board(first, ALICE, 1000);
        board(first, BOB, 1020);
        board(second, ALICE, 2000);
        board(second, BOB, 1000);
        board(second, CAROL, 1000);
        board(third, ALICE, 5000);

        var page = client.getPlayerTopTimes(ALICE, 0, 2);
        assertEquals(3, page.count());
        assertEquals(
            Set.of(first, third),
            page.results().stream().map(PlayerTopTime::mapId).collect(Collectors.toSet()),
            "both rank 1; the order between them is the map's"
        );
        var onFirst = page.results()
            .stream()
            .filter(t -> t.mapId().equals(first))
            .findFirst()
            .orElseThrow();
        assertEquals(1, onFirst.rank());
        assertEquals(1000, onFirst.completionTime());
        assertEquals("000-000-101", onFirst.publishedId());
        assertEquals("A map", onFirst.mapName());

        var last = client.getPlayerTopTimes(ALICE, 1, 2);
        assertEquals(3, last.count());
        assertEquals(List.of(second), last.results().stream().map(t -> t.mapId()).toList());
        assertEquals(3, last.results().getFirst().rank());
        assertTrue(client.getPlayerTopTimes(ALICE, 5, 2).results().isEmpty());
        assertEquals(0, client.getPlayerTopTimes(DAVE, 0, 10).count());
    }

    @Test
    void delete_softDeletesTheRunsDropsThePlayerFromTheBoardAndNotifies() {
        var map = map("parkour", true, null);
        run(map, ALICE, 1000, null);
        run(map, BOB, 2000, null);
        board(map, ALICE, 1000);
        board(map, BOB, 2000);

        client.deleteLeaderboardEntry(map, ALICE, true);

        assertEquals(1, count("select count(*) from save_states where deleted is null"));
        assertNull(score(map, ALICE));
        assertEquals(2000.0, score(map, BOB));
        assertEquals(
            1,
            count(
                "select count(*) from player_notifications where type = 'map_time_deleted' and key = '"
                    + map
                    + "' and player_id = '"
                    + ALICE
                    + "'"
            )
        );
        assertTrue(
            clients.messages.stream().anyMatch(m -> m.subject().equals("notification.created"))
        );

        client.deleteLeaderboardEntry(map, BOB, false);
        assertEquals(1, count("select count(*) from player_notifications"));
    }

    @Test
    void deletePlayer_dropsThePlayerFromEveryBoard() {
        var first = map("parkour", true, null);
        var second = map("parkour", true, null);
        run(first, ALICE, 1000, null);
        run(second, ALICE, 1000, null);
        run(second, BOB, 1000, null);
        board(first, ALICE, 1000);
        board(second, ALICE, 1000);
        board(second, BOB, 1000);

        client.deletePlayerLeaderboardEntries(ALICE);

        assertEquals(1, count("select count(*) from save_states where deleted is null"));
        assertNull(score(first, ALICE));
        assertNull(score(second, ALICE));
        assertEquals(1000.0, score(second, BOB));
    }

    @Test
    void deleteMap_softDeletesEveryRunAndDropsTheBoard() {
        var map = map("parkour", true, null);
        var other = map("parkour", true, null);
        run(map, ALICE, 1000, null);
        run(map, BOB, 1000, null);
        run(other, ALICE, 1000, null);
        board(map, ALICE, 1000);
        board(other, ALICE, 1000);

        client.deleteLeaderboard(map);

        assertEquals(1, count("select count(*) from save_states where deleted is null"));
        assertNull(score(map, ALICE));
        assertEquals(1000.0, score(other, ALICE));
    }

    @Test
    void undelete_restoresRunsDeletedInTheRangeAndRebuildsTheBoard() {
        var map = map("parkour", true, null);
        run(map, ALICE, 1000, Instant.parse("2026-01-01T00:00:00Z"));
        run(map, ALICE, 2000, Instant.parse("2026-02-01T00:00:00Z"));
        run(map, ALICE, 3000, Instant.parse("2026-03-01T00:00:00Z"));
        run(map, BOB, 500, Instant.parse("2026-02-01T00:00:00Z"));

        var restored = client.undeleteLeaderboardEntry(
            map,
            ALICE,
            Instant.parse("2026-01-15T00:00:00Z"),
            Instant.parse("2026-02-15T00:00:00Z")
        );

        assertEquals(1, restored);
        assertEquals(2000.0, score(map, ALICE));
        assertNull(score(map, BOB));
        assertEquals(2, client.undeleteLeaderboardEntry(map, ALICE, null, null));
        assertEquals(1000.0, score(map, ALICE));
        assertEquals(0, client.undeleteLeaderboardEntry(map, ALICE, null, null));
    }

    @Test
    void undeletePlayer_countsRunsAcrossMapsAndRebuildsEachBoard() {
        var first = map("parkour", true, null);
        var second = map("parkour", true, null);
        run(first, ALICE, 1000, Instant.parse("2026-01-01T00:00:00Z"));
        run(second, ALICE, 2000, Instant.parse("2026-01-01T00:00:00Z"));
        run(second, ALICE, 4000, Instant.parse("2025-01-01T00:00:00Z"));

        assertEquals(
            2,
            client.undeletePlayerLeaderboardEntries(
                ALICE,
                Instant.parse("2026-01-01T00:00:00Z"),
                null
            )
        );
        assertEquals(1000.0, score(first, ALICE));
        assertEquals(2000.0, score(second, ALICE));
    }

    @Test
    void undeleteMap_restoresEveryRunDeletedBeforeTheBound() {
        var map = map("parkour", true, null);
        run(map, ALICE, 1000, Instant.parse("2026-01-01T00:00:00Z"));
        run(map, BOB, 2000, Instant.parse("2026-01-01T00:00:00Z"));
        run(map, CAROL, 3000, Instant.parse("2026-06-01T00:00:00Z"));

        assertEquals(
            2,
            client.undeleteLeaderboard(map, null, Instant.parse("2026-02-01T00:00:00Z"))
        );
        assertEquals(1000.0, score(map, ALICE));
        assertEquals(2000.0, score(map, BOB));
        assertNull(score(map, CAROL));
    }

    @Test
    void rebuild_rewritesTheBoardFromEachPlayersBestRun() {
        var map = map("parkour", true, null);
        run(map, ALICE, 3000, null);
        run(map, ALICE, 1000, null);
        run(map, BOB, 2000, Instant.now());
        board(map, CAROL, 100);

        client.rebuildLeaderboard(map);

        assertEquals(1000.0, score(map, ALICE));
        assertNull(score(map, BOB));
        assertNull(score(map, CAROL));
    }

    @Test
    void rebuild_leavesABuildingMapAloneAndRefusesAnUnknownOne() {
        var building = map("building", true, null);
        board(building, ALICE, 100);
        client.rebuildLeaderboard(building);
        assertEquals(100.0, score(building, ALICE));

        var refused = assertThrows(
            IpcException.class,
            () -> client.rebuildLeaderboard(UUID.randomUUID())
        );
        assertEquals(404, refused.status());
    }

    private void board(UUID map, UUID player, double score) {
        clients.redis.zadd(
            MapCompat.leaderboardKey(map),
            score,
            MapCompat.leaderboardMember(player)
        );
    }

    private @Nullable Double score(UUID map, UUID player) {
        return clients.redis.zscore(
            MapCompat.leaderboardKey(map),
            MapCompat.leaderboardMember(player)
        );
    }

    private void run(UUID map, UUID player, long playtime, @Nullable Instant deleted) {
        TEST_DB.seed(
            """
            insert into save_states (id, map_id, player_id, type, created, updated, deleted, completed, playtime,
                                     state_v2)
            values ('%s', '%s', '%s', 'playing', now(), now(), %s, true, %s, 'null')
            """
                .formatted(
                    UUID.randomUUID(),
                    map,
                    player,
                    deleted == null ? "null" : "'" + deleted + "'",
                    playtime
                )
        );
    }

    private UUID map(String variant, boolean published, @Nullable String leaderboard) {
        var id = UUID.randomUUID();
        TEST_DB.seed(
            """
            insert into maps (id, owner, m_type, created_at, updated_at, authz_key, file_id, legacy_map_id,
                              opt_name, opt_icon, opt_variant, opt_spawn_point, size, protocol_version,
                              published_at, published_id, leaderboard)
            values ('%s', '%s', 'default', now(), now(), '', '', '', 'A map', '', '%s',
                    '{"x":0,"y":40,"z":0,"yaw":90,"pitch":0}', 1, 776, %s, %s, %s)
            """
                .formatted(
                    id,
                    ALICE,
                    variant,
                    published ? "now()" : "null",
                    published ? String.valueOf(publishedIds.incrementAndGet()) : "null",
                    leaderboard == null ? "null" : "'" + leaderboard + "'"
                )
        );
        return id;
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
