package net.hollowcube.apiserver.map;

import com.sun.net.httpserver.HttpServer;
import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.apiserver.s3.MemoryS3Client;
import net.hollowcube.ipc.map.GlobalLeaderboard;
import net.hollowcube.ipc.map.LeaderboardData;
import net.hollowcube.ipc.map.MapClient;
import net.hollowcube.ipc.map.MapServer;
import net.hollowcube.ipc.map.PlayerTopTime;
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
    static final TestDb TEST_DB = TestDb.of("../../modules/api/src/main/sql/migrations");
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
                    + player + "', 'p" + player.toString().charAt(35) + "', now(), now(), false)"
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
            new MemoryS3Client(),
            clients.nats,
            clients.redis,
            clients.posthog,
            Duration.ZERO
        );
    }

    @Test
    void getMapLeaderboard_roundsTimesToTheTickAndSharesRanksOnTiesInAndOutsideTheTop() {
        var map = map("parkour", true, null);
        board(map, ALICE, 1000);
        board(map, BOB, 1020);
        board(map, CAROL, 1049);
        for (var i = 0; i < 8; i++) board(map, UUID.randomUUID(), 2000 + i * 100);
        board(map, DAVE, 2710);

        var leaderboard = client.getMapLeaderboard(map, DAVE);
        assertEquals(10, leaderboard.top().size());
        assertEquals(
            List.of(
                new LeaderboardData.Entry(ALICE, 1000, 1),
                new LeaderboardData.Entry(BOB, 1000, 1),
                new LeaderboardData.Entry(CAROL, 1050, 3)
            ),
            leaderboard.top().subList(0, 3)
        );
        assertEquals(
            new LeaderboardData.Entry(DAVE, 2700, 11),
            leaderboard.player(),
            "outside the top, a time shown equal to the 11th ties with it"
        );
    }

    @Test
    void getMapLeaderboard_readsANumberBoardInItsDirectionRankingWhatItShows() {
        var descending = map(
            "parkour",
            true,
            "{\"asc\": false, \"format\": \"number\", \"score\": \"q.coins\"}"
        );
        board(descending, ALICE, 7.5);
        board(descending, BOB, 90.0);
        board(descending, CAROL, 87.5);
        board(descending, DAVE, 90.0);

        var leaderboard = client.getMapLeaderboard(descending, ALICE);
        assertEquals(
            List.of(
                new LeaderboardData.Entry(DAVE, 90, 1),
                new LeaderboardData.Entry(BOB, 90, 1),
                new LeaderboardData.Entry(CAROL, 87, 3),
                new LeaderboardData.Entry(ALICE, 7, 4)
            ),
            leaderboard.top(),
            "redis orders a tie by member, so the higher uuid comes first off a reversed range"
        );
        assertEquals(
            new LeaderboardData.Entry(ALICE, 7, 4),
            leaderboard.player(),
            "a score only beats what is shown as a whole number below it, never itself"
        );
        assertEquals(
            new LeaderboardData.Entry(CAROL, 87, 3),
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

        var last = client.getPlayerTopTimes(ALICE, 1, 2);
        assertEquals(3, last.count());
        assertEquals(List.of(second), last.results().stream().map(t -> t.mapId()).toList());
        assertEquals(3, last.results().getFirst().rank());
    }

    @Test
    void delete_softDeletesRunsDropsThemFromTheBoardAndRefreshesStats() {
        var first = map("parkour", true, null);
        var second = map("parkour", true, null);
        for (var player : List.of(ALICE, BOB, CAROL, DAVE)) {
            run(first, player, 1000, null);
            board(first, player, 1000);
        }
        for (var player : List.of(ALICE, CAROL)) {
            run(second, player, 1000, null);
            board(second, player, 1000);
        }

        client.deleteLeaderboardEntry(first, ALICE, true);
        assertNull(score(first, ALICE));
        assertEquals(1000.0, score(first, BOB));
        assertEquals(3, count("select win_count from map_stats where map_id = '" + first + "'"));
        assertEquals(
            1,
            count(
                "select count(*) from player_notifications where type = 'map_time_deleted' and key = '"
                    + first + "' and player_id = '" + ALICE + "'"
            )
        );
        assertTrue(
            clients.messages.stream().anyMatch(m -> m.subject().equals("notification.created"))
        );
        client.deleteLeaderboardEntry(first, BOB, false);
        assertEquals(1, count("select count(*) from player_notifications"));

        client.deletePlayerLeaderboardEntries(CAROL);
        assertNull(score(first, CAROL));
        assertNull(score(second, CAROL));
        assertEquals(1000.0, score(second, ALICE));

        client.deleteLeaderboard(second);
        assertNull(score(second, ALICE));
        assertEquals(1000.0, score(first, DAVE));
        assertEquals(1, count("select count(*) from save_states where deleted_at is null"));
    }

    @Test
    void undelete_restoresRunsDeletedInTheRangeAndRebuildsTheBoard() {
        var first = map("parkour", true, null);
        var second = map("parkour", true, null);
        run(first, ALICE, 1000, Instant.parse("2026-01-01T00:00:00Z"));
        run(first, ALICE, 2000, Instant.parse("2026-02-01T00:00:00Z"));
        run(first, ALICE, 3000, Instant.parse("2026-03-01T00:00:00Z"));
        run(first, BOB, 500, Instant.parse("2026-02-01T00:00:00Z"));
        run(second, ALICE, 4000, Instant.parse("2025-01-01T00:00:00Z"));
        run(second, BOB, 1500, Instant.parse("2026-01-01T00:00:00Z"));
        run(second, CAROL, 3000, Instant.parse("2026-06-01T00:00:00Z"));

        assertEquals(
            1,
            client.undeleteLeaderboardEntry(
                first,
                ALICE,
                Instant.parse("2026-01-15T00:00:00Z"),
                Instant.parse("2026-02-15T00:00:00Z")
            )
        );
        assertEquals(2000.0, score(first, ALICE));
        assertNull(score(first, BOB));
        assertEquals(1, count("select win_count from map_stats where map_id = '" + first + "'"));

        assertEquals(
            2,
            client.undeletePlayerLeaderboardEntries(
                ALICE,
                Instant.parse("2026-01-01T00:00:00Z"),
                null
            )
        );
        assertEquals(1000.0, score(first, ALICE));
        assertNull(score(second, ALICE));

        assertEquals(
            2,
            client.undeleteLeaderboard(second, null, Instant.parse("2026-02-01T00:00:00Z"))
        );
        assertEquals(4000.0, score(second, ALICE));
        assertEquals(1500.0, score(second, BOB));
        assertNull(score(second, CAROL));
    }

    @Test
    void rebuild_rewritesTheBoardFromEachPlayersBestRunButLeavesABuildingMapAlone() {
        var map = map("parkour", true, null);
        run(map, ALICE, 3000, null);
        run(map, ALICE, 1000, null);
        run(map, BOB, 2000, Instant.now());
        board(map, CAROL, 100);
        client.rebuildLeaderboard(map);
        assertEquals(1000.0, score(map, ALICE));
        assertNull(score(map, BOB));
        assertNull(score(map, CAROL));

        var building = map("building", true, null);
        board(building, ALICE, 100);
        client.rebuildLeaderboard(building);
        assertEquals(100.0, score(building, ALICE));
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
            insert into save_states (id, map_id, player_id, type, created_at, updated_at, deleted_at, completed,
                                     playtime, state)
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
            insert into maps (id, owner, created_at, updated_at, protocol_version, published_at, published_id,
                              leaderboard, name, icon, variant, spawn_point, size_class, verification_state, quality)
            values ('%s', '%s', now(), now(), 776, %s, %s, %s, 'A map', '', '%s',
                    '{"x":0,"y":40,"z":0,"yaw":90,"pitch":0}', 'large', 'unverified', 'unrated')
            """
                .formatted(
                    id,
                    ALICE,
                    published ? "now()" : "null",
                    published ? String.valueOf(publishedIds.incrementAndGet()) : "null",
                    leaderboard == null ? "null" : "'" + leaderboard + "'",
                    variant
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
