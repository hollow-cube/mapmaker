package net.hollowcube.apiserver.map;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpServer;
import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.apiserver.s3.MemoryS3Client;
import net.hollowcube.ipc.map.DeleteVerificationResult;
import net.hollowcube.ipc.map.MapClient;
import net.hollowcube.ipc.map.MapServer;
import net.hollowcube.ipc.map.SaveStateType;
import net.hollowcube.ipc.map.SaveStateUpdate;
import net.hollowcube.ipc.util.IpcException;
import net.hollowcube.sqlgen.testing.TestDb;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/// The save state calls through the ipc client against the embedded database, with the board in
/// a fake redis so that what a write did to it can be asserted.
class MapSaveStateTest {

    @RegisterExtension
    static final TestDb TEST_DB = TestDb.of(
        "../../modules/api/src/main/sql/migrations",
        TestDb.Mode.TRUNCATE
    );
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-000000000002");
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
    void upsert_insertsAtTheClientsCreatedTime() {
        var map = map("parkour");
        var id = UUID.randomUUID();
        var created = Instant.parse("2026-09-01T10:00:00Z");
        var state = new JsonObject();
        state.addProperty("x", 1);

        client.upsertSaveState(
            map,
            PLAYER,
            id,
            SaveStateUpdate.builder(SaveStateType.PLAYING)
                .created(created)
                .playtime(1500)
                .ticks(30)
                .attempts(2, 9000)
                .dataVersion(4000)
                .protocolVersion(776)
                .state(state)
                .build()
        );

        var latest = client.getLatestSaveState(map, PLAYER, SaveStateType.PLAYING);
        assertNotNull(latest);
        assertEquals(id, latest.id());
        assertEquals(created, latest.created());
        assertEquals(1500, latest.playtime());
        assertEquals(30, latest.ticks());
        assertEquals(2, latest.resets());
        assertEquals(9000, latest.totalPlaytime());
        assertEquals(4000, latest.dataVersion());
        assertEquals(776, latest.protocolVersion());
        assertEquals(state, latest.state());
        assertFalse(latest.completed());
        assertNull(latest.score());
    }

    @Test
    void upsert_guessesCreatedFromPlaytimeWhenNoneIsSent() {
        var map = map("parkour");
        var before = Instant.now().minus(Duration.ofMillis(60_000));
        client.upsertSaveState(map, PLAYER, UUID.randomUUID(), playing(60_000).build());

        var created = client.getLatestSaveState(map, PLAYER, SaveStateType.PLAYING).created();
        assertFalse(created.isBefore(before.minusSeconds(5)));
        assertFalse(created.isAfter(before.plusSeconds(5)));
    }

    @Test
    void upsert_keepsTheRowsTypeStateAndCreatedAcrossSaves() {
        var map = map("parkour");
        var id = UUID.randomUUID();
        var state = new JsonObject();
        state.addProperty("checkpoint", 3);
        client.upsertSaveState(map, PLAYER, id, playing(1000).state(state).build());
        var created = client.getLatestSaveState(map, PLAYER, SaveStateType.PLAYING).created();

        client.upsertSaveState(
            map,
            PLAYER,
            id,
            SaveStateUpdate.builder(SaveStateType.EDITING)
                .created(Instant.EPOCH)
                .playtime(2000)
                .attempts(0, 500)
                .build()
        );

        var latest = client.getLatestSaveState(map, PLAYER, SaveStateType.PLAYING);
        assertEquals(SaveStateType.PLAYING, latest.type());
        assertEquals(created, latest.created());
        assertEquals(state, latest.state());
        assertEquals(2000, latest.playtime());
        assertEquals(2000, latest.totalPlaytime(), "playtime is the floor of the lineage total");
    }

    @Test
    void upsert_refusesACompletedState() {
        var map = map("parkour");
        var id = UUID.randomUUID();
        client.upsertSaveState(map, PLAYER, id, playing(1000).completed(true, null).build());

        var refused = assertThrows(
            IpcException.class,
            () -> client.upsertSaveState(map, PLAYER, id, playing(2000).build())
        );
        assertEquals(409, refused.status());
        assertEquals(1000, db.saveStates.get(id, map, PLAYER).playtime());
    }

    @Test
    void upsert_unknownMapIs404() {
        var refused = assertThrows(
            IpcException.class,
            () -> client.upsertSaveState(
                UUID.randomUUID(),
                PLAYER,
                UUID.randomUUID(),
                playing(1).build()
            )
        );
        assertEquals(404, refused.status());
    }

    @Test
    void upsert_completionDropsTheStateAndScoresByPlaytime() {
        var map = map("parkour");
        var id = UUID.randomUUID();
        var state = new JsonObject();
        state.addProperty("x", 1);
        client.upsertSaveState(
            map,
            PLAYER,
            id,
            playing(1000).ticks(30).state(state).completed(true, null).build()
        );

        var row = db.saveStates.get(id, map, PLAYER);
        assertTrue(row.completed());
        assertEquals("null", new String(row.stateV2()));
        assertEquals(1500.0, row.score(), "ticks are the floor of a completed run's time");
        assertNull(client.getLatestSaveState(map, PLAYER, SaveStateType.PLAYING));
        assertNull(client.getBestSaveState(map, PLAYER).state());
        assertEquals(1500.0, client.getBestSaveState(map, PLAYER).score());
    }

    @Test
    void upsert_completedParkourRunLandsOnTheBoardAndKeepsTheBest() {
        var map = map("parkour");
        client.upsertSaveState(
            map,
            PLAYER,
            UUID.randomUUID(),
            playing(3000).completed(true, 3000.0).build()
        );
        client.upsertSaveState(
            map,
            PLAYER,
            UUID.randomUUID(),
            playing(5000).completed(true, 5000.0).build()
        );
        client.upsertSaveState(
            map,
            PLAYER,
            UUID.randomUUID(),
            playing(2000).completed(true, 2000.0).build()
        );

        assertEquals(2000.0, board(map, PLAYER));
        assertEquals(3, clients.finishAnalytics().stream().filter("map_completed"::equals).count());
    }

    @Test
    void upsert_descendingBoardKeepsTheHighestScore() {
        var map = map("parkour");
        TEST_DB.seed(
            "update maps set leaderboard = '{\"asc\": false, \"format\": \"number\", \"score\": \"q.coins\"}' where id = '"
                + map
                + "'"
        );
        client.upsertSaveState(
            map,
            PLAYER,
            UUID.randomUUID(),
            playing(1000).completed(true, 10.0).build()
        );
        client.upsertSaveState(
            map,
            PLAYER,
            UUID.randomUUID(),
            playing(1000).completed(true, 4.0).build()
        );

        assertEquals(10.0, board(map, PLAYER));
    }

    @Test
    void upsert_editingStatesAndBuildingMapsNeverTouchTheBoard() {
        var parkour = map("parkour");
        var building = map("building");
        client.upsertSaveState(
            parkour,
            PLAYER,
            UUID.randomUUID(),
            SaveStateUpdate.builder(SaveStateType.EDITING)
                .playtime(1000)
                .completed(true, null)
                .build()
        );
        client.upsertSaveState(
            building,
            PLAYER,
            UUID.randomUUID(),
            playing(1000).completed(true, null).build()
        );

        assertNull(board(parkour, PLAYER));
        assertNull(board(building, PLAYER));
        assertNull(
            db.saveStates
                .get(
                    db.saveStates
                        .latest(parkour, PLAYER, net.hollowcube.apiserver.db.SaveStateType.EDITING)
                        .id(),
                    parkour,
                    PLAYER
                )
                .score(),
            "an editing state is never scored"
        );
        assertTrue(clients.finishAnalytics().isEmpty());
    }

    @Test
    void upsert_completingAVerificationMarksTheMapVerifiedAtThatVersion() {
        var map = map("parkour");
        TEST_DB.seed("update maps set verification = 1 where id = '" + map + "'");
        client.upsertSaveState(
            map,
            PLAYER,
            UUID.randomUUID(),
            SaveStateUpdate.builder(SaveStateType.VERIFYING)
                .playtime(1000)
                .protocolVersion(777)
                .completed(true, null)
                .build()
        );

        var row = db.maps.getMap(map);
        assertEquals(2L, row.verification());
        assertEquals(777, row.protocolVersion());
        assertEquals(1000.0, board(map, PLAYER));
    }

    @Test
    void upsert_doesNotVerifyOrBoardAVerificationTheResetAlreadyStruck() {
        var map = map("parkour");
        var state = UUID.randomUUID();
        TEST_DB.seed("update maps set verification = 1 where id = '" + map + "'");
        client.upsertSaveState(
            map,
            PLAYER,
            state,
            SaveStateUpdate.builder(SaveStateType.VERIFYING)
                .playtime(400)
                .protocolVersion(777)
                .build()
        );

        assertEquals(DeleteVerificationResult.RESET, client.deleteVerification(map));

        client.upsertSaveState(
            map,
            PLAYER,
            state,
            SaveStateUpdate.builder(SaveStateType.VERIFYING)
                .playtime(1000)
                .protocolVersion(777)
                .completed(true, null)
                .build()
        );

        assertEquals(0L, db.maps.getMap(map).verification());
        assertNull(board(map, PLAYER), "the struck run must not come back on the board");
        assertNull(client.getBestSaveState(map, PLAYER));
    }

    @Test
    void upsert_refreshesTheMapsStats() {
        var map = map("parkour");
        client.upsertSaveState(map, PLAYER, UUID.randomUUID(), playing(1000).build());
        client.upsertSaveState(
            map,
            OTHER,
            UUID.randomUUID(),
            playing(1000).completed(true, null).build()
        );
        client.upsertSaveState(
            map,
            OTHER,
            UUID.randomUUID(),
            playing(1000).completed(true, null).build()
        );

        var stats = db.maps.getStats(map);
        assertEquals(2, stats.playCount());
        assertEquals(1, stats.winCount());
    }

    @Test
    void getLatest_isTheNewestOfTheTypeAndNullOnceCompleted() {
        var map = map("parkour");
        var first = UUID.randomUUID();
        var second = UUID.randomUUID();
        client.upsertSaveState(map, PLAYER, first, playing(1000).build());
        client.upsertSaveState(map, PLAYER, second, playing(1000).build());
        TEST_DB.seed(
            "update save_states set updated = updated - interval '1 minute' where id = '"
                + second
                + "'"
        );
        assertEquals(first, client.getLatestSaveState(map, PLAYER, SaveStateType.PLAYING).id());
        assertNull(client.getLatestSaveState(map, PLAYER, SaveStateType.EDITING));

        client.upsertSaveState(map, PLAYER, first, playing(1000).completed(true, null).build());
        assertNull(client.getLatestSaveState(map, PLAYER, SaveStateType.PLAYING));
    }

    @Test
    void getLatest_readsRowsGoWroteBeforeTheNewerColumns() {
        var map = map("parkour");
        var id = UUID.randomUUID();
        TEST_DB.seed(
            """
            insert into save_states (id, map_id, player_id, type, created, updated, completed, playtime, ticks,
                                     state_v2, protocol_version)
            values ('%s', '%s', '%s', 'playing', now(), now(), false, 4000, 100, 'not json', null)
            """
                .formatted(id, map, PLAYER)
        );

        var latest = client.getLatestSaveState(map, PLAYER, SaveStateType.PLAYING);
        assertEquals(769, latest.protocolVersion());
        assertEquals(4000, latest.totalPlaytime());
        assertEquals(new JsonObject(), latest.state());
    }

    @Test
    void getBest_honoursTheBoardsDirection() {
        var map = map("parkour");
        client.upsertSaveState(
            map,
            PLAYER,
            UUID.randomUUID(),
            playing(3000).completed(true, 3000.0).build()
        );
        client.upsertSaveState(
            map,
            PLAYER,
            UUID.randomUUID(),
            playing(2000).completed(true, 2000.0).build()
        );
        assertEquals(2000.0, client.getBestSaveState(map, PLAYER).score());

        TEST_DB.seed(
            "update maps set leaderboard = '{\"asc\": false, \"format\": \"time\", \"score\": \"q.playtime\"}' where id = '"
                + map
                + "'"
        );
        assertEquals(3000.0, client.getBestSaveState(map, PLAYER).score());
        assertNull(client.getBestSaveState(map, OTHER));
    }

    /// `select save_states.*` is read by column position, so the mirror has to leave the columns in
    /// the order production has them: Go's `000001` and `000002`, then the `data_version` of its
    /// `000009`, the `protocol_version` of `000015`, the `ticks` of `000016`, the `score` of
    /// `000030`, and the `resets` and `total_playtime` of `000036`. Declaring the same columns in
    /// one tidy statement would read every row off by a field against a real database while every
    /// test here passed, which is what happened to `replay_segments`.
    @Test
    void migration_ordersSaveStatesAsProductionHasThem() {
        assertEquals(
            List.of(
                "id",
                "map_id",
                "player_id",
                "type",
                "created",
                "updated",
                "deleted",
                "completed",
                "playtime",
                "state_v2",
                "data_version",
                "protocol_version",
                "ticks",
                "score",
                "resets",
                "total_playtime"
            ),
            columnsOf("save_states")
        );
    }

    /// `pg_attribute` rather than `information_schema`, which pglite does not serve.
    private static List<String> columnsOf(String table) {
        var sql = """
            select a.attname
            from pg_attribute a
                     join pg_class c on c.oid = a.attrelid
                     join pg_namespace n on n.oid = c.relnamespace
            where n.nspname = 'public' and c.relname = '%s'
              and a.attnum > 0 and not a.attisdropped
            order by a.attnum"""
            .formatted(table);
        try (
            var statement = TEST_DB.conn().createStatement();
            var rows = statement.executeQuery(sql)
        ) {
            var columns = new ArrayList<String>();
            while (rows.next()) columns.add(rows.getString(1));
            return columns;
        } catch (SQLException e) {
            throw new IllegalStateException("could not read the columns of " + table, e);
        }
    }

    private static SaveStateUpdate.Builder playing(long playtime) {
        return SaveStateUpdate.builder(SaveStateType.PLAYING)
            .playtime(playtime)
            .protocolVersion(776);
    }

    private Double board(UUID map, UUID player) {
        return clients.redis.zscore(
            MapCompat.leaderboardKey(map),
            MapCompat.leaderboardMember(player)
        );
    }

    private UUID map(String variant) {
        var id = UUID.randomUUID();
        TEST_DB.seed(
            """
            insert into maps (id, owner, m_type, created_at, updated_at, authz_key, file_id, legacy_map_id,
                              opt_name, opt_icon, opt_variant, opt_spawn_point, size, protocol_version)
            values ('%s', '%s', 'default', now(), now(), '', '', '', 'A map', '', '%s',
                    '{"x":0,"y":40,"z":0,"yaw":90,"pitch":0}', 1, 776)
            """
                .formatted(id, PLAYER, variant)
        );
        return id;
    }
}
