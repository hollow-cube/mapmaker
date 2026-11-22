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
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/// The save state calls through the ipc client against the embedded database, with the board in
/// a fake redis so that what a write did to it can be asserted.
class MapSaveStateTest {

    @RegisterExtension
    static final TestDb TEST_DB = TestDb.of("../../modules/api/src/main/sql/migrations");
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
            new MemoryS3Client(),
            clients.nats,
            clients.redis,
            clients.posthog,
            Duration.ZERO
        );
    }

    @Test
    void upsert_keepsTheRowsTypeStateAndCreatedAcrossSaves() {
        var map = map("parkour");
        var id = UUID.randomUUID();
        var created = Instant.parse("2026-09-01T10:00:00Z");
        var state = new JsonObject();
        state.addProperty("name", "a\u0000b");
        client.upsertSaveState(map, PLAYER, id, playing(1000).created(created).state(state).build());

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
        var stored = new JsonObject();
        stored.addProperty("name", "ab");
        assertEquals(SaveStateType.PLAYING, latest.type());
        assertEquals(created, latest.created());
        assertEquals(stored, latest.state());
        assertEquals(2000, latest.playtime());
        assertEquals(2000, latest.totalPlaytime(), "playtime is the floor of the lineage total");
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
    void upsert_refusesACompletedStateOrAnIdAnotherPlayerHolds() {
        var map = map("parkour");
        var completed = UUID.randomUUID();
        client.upsertSaveState(map, PLAYER, completed, playing(1000).completed(true, null).build());
        assertEquals(
            409,
            assertThrows(
                IpcException.class,
                () -> client.upsertSaveState(map, PLAYER, completed, playing(2000).build())
            ).status()
        );
        assertEquals(1000, db.saveStates.get(completed, map, PLAYER).playtime());

        var held = UUID.randomUUID();
        client.upsertSaveState(map, PLAYER, held, playing(1000).build());
        assertEquals(
            409,
            assertThrows(
                IpcException.class,
                () -> client.upsertSaveState(map, OTHER, held, playing(2000).build())
            ).status()
        );
        assertEquals(1000, db.saveStates.get(held, map, PLAYER).playtime());
        assertNull(db.saveStates.get(held, map, OTHER));
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
        assertEquals("null", row.state());
        assertEquals(1500.0, row.score(), "ticks are the floor of a completed run's time");
        assertNull(client.getBestSaveState(map, PLAYER).state());
        assertEquals(1500.0, client.getBestSaveState(map, PLAYER).score());
    }

    @Test
    void upsert_completedRunsKeepTheBestScoreInTheBoardsDirection() {
        var ascending = map("parkour");
        for (long score : new long[] {3000, 5000, 2000})
            client.upsertSaveState(
                ascending,
                PLAYER,
                UUID.randomUUID(),
                playing(score).completed(true, (double) score).build()
            );
        assertEquals(2000.0, board(ascending, PLAYER));
        assertEquals(2000.0, client.getBestSaveState(ascending, PLAYER).score());

        var descending = map("parkour");
        TEST_DB.seed(
            "update maps set leaderboard = '{\"asc\": false, \"format\": \"number\", \"score\": \"q.coins\"}' where id = '"
                + descending + "'"
        );
        for (long score : new long[] {10, 4})
            client.upsertSaveState(
                descending,
                PLAYER,
                UUID.randomUUID(),
                playing(1000).completed(true, (double) score).build()
            );
        assertEquals(10.0, board(descending, PLAYER));
        assertEquals(10.0, client.getBestSaveState(descending, PLAYER).score());
        assertEquals(5, clients.finishAnalytics().stream().filter("map_completed"::equals).count());
    }

    @Test
    void upsert_editingStatesAndBuildingMapsNeverTouchTheBoard() {
        var parkour = map("parkour");
        var building = map("building");
        var editing = UUID.randomUUID();
        client.upsertSaveState(
            parkour,
            PLAYER,
            editing,
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
        assertNull(db.saveStates.get(editing, parkour, PLAYER).score());
        assertTrue(clients.finishAnalytics().isEmpty());
    }

    @Test
    void upsert_completingAVerificationVerifiesTheMapUnlessAResetStruckIt() {
        var map = map("parkour");
        var struck = UUID.randomUUID();
        TEST_DB.seed("update maps set verification_state = 'pending' where id = '" + map + "'");
        client.upsertSaveState(
            map,
            PLAYER,
            struck,
            SaveStateUpdate.builder(SaveStateType.VERIFYING)
                .playtime(400)
                .protocolVersion(777)
                .build()
        );
        assertEquals(DeleteVerificationResult.RESET, client.deleteVerification(map));
        client.upsertSaveState(
            map,
            PLAYER,
            struck,
            SaveStateUpdate.builder(SaveStateType.VERIFYING)
                .playtime(1000)
                .protocolVersion(777)
                .completed(true, null)
                .build()
        );
        assertEquals(
            net.hollowcube.apiserver.db.MapVerification.UNVERIFIED,
            db.maps.getMap(map).verificationState()
        );
        assertNull(board(map, PLAYER), "the struck run must not come back on the board");
        assertNull(client.getBestSaveState(map, PLAYER));

        TEST_DB.seed("update maps set verification_state = 'pending' where id = '" + map + "'");
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
        assertEquals(net.hollowcube.apiserver.db.MapVerification.VERIFIED, row.verificationState());
        assertEquals(777, row.protocolVersion());
        assertEquals(1000.0, board(map, PLAYER));
    }

    @Test
    void getLatest_isTheNewestOfTheTypeAndNullOnceCompleted() {
        var map = map("parkour");
        var first = UUID.randomUUID();
        var second = UUID.randomUUID();
        client.upsertSaveState(map, PLAYER, first, playing(1000).build());
        client.upsertSaveState(map, PLAYER, second, playing(1000).build());
        TEST_DB.seed(
            "update save_states set updated_at = updated_at - interval '1 minute' where id = '"
                + second + "'"
        );
        assertEquals(first, client.getLatestSaveState(map, PLAYER, SaveStateType.PLAYING).id());
        assertNull(client.getLatestSaveState(map, PLAYER, SaveStateType.EDITING));

        client.upsertSaveState(map, PLAYER, first, playing(1000).completed(true, null).build());
        assertNull(client.getLatestSaveState(map, PLAYER, SaveStateType.PLAYING));
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
            insert into maps (id, owner, created_at, updated_at, protocol_version, name, icon, variant, spawn_point,
                              size_class, verification_state, quality)
            values ('%s', '%s', now(), now(), 776, 'A map', '', '%s', '{"x":0,"y":40,"z":0,"yaw":90,"pitch":0}',
                    'large', 'unverified', 'unrated')
            """
                .formatted(id, PLAYER, variant)
        );
        return id;
    }
}
