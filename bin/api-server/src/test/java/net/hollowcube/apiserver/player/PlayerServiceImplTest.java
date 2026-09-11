package net.hollowcube.apiserver.player;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.ipc.player.DisplayName;
import net.hollowcube.ipc.player.PlayerClient;
import net.hollowcube.ipc.player.PlayerServer;
import net.hollowcube.ipc.player.PlayerStub;
import net.hollowcube.ipc.util.IpcException;
import net.hollowcube.sqlgen.testing.TestDb;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerServiceImplTest {

    @RegisterExtension
    static final TestDb TEST_DB = TestDb.of("../../modules/api/src/main/sql/migrations");

    private static final UUID ALICE = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BOB = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID CAROL = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID MOD = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private HttpServer server;
    private PlayerClient players;

    @BeforeEach
    void start() throws IOException {
        TEST_DB.seed(
            """
            insert into player_data (id, username, first_join, last_online, online, settings, role, hypercube_start, hypercube_end, extra_map_slots, map_builders) values
                ('%s', 'Alice', now(), now(), true, '{"a": 1, "b": true}', 'default', null, null, 1, 0),
                ('%s', 'bobby', now(), now(), false, '{}', 'default', now() - interval '1 day', now() + interval '1 day', 0, 0),
                ('%s', 'Carol', now(), now(), false, '{}', 'default', now() - interval '2 day', now() - interval '1 day', 0, 2),
                ('%s', 'Moddy', now(), now(), false, '{}', 'mod_1', null, null, 0, 0);
            insert into ip_history (player_id, address, first_seen, last_seen, seen_count) values
                ('%s', '10.0.0.1', now(), now(), 1),
                ('%s', '10.0.0.2', now(), now(), 1),
                ('%s', '10.0.0.1', now(), now(), 1),
                ('%s', '10.0.0.2', now(), now(), 1)"""
                .formatted(ALICE, BOB, CAROL, MOD, ALICE, ALICE, BOB, CAROL)
        );

        var service = new PlayerServiceImpl(TEST_DB.database(ApiDatabase::new));
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(PlayerServer.PATH, new PlayerServer(service));
        server.start();
        players = new PlayerClient(
            HttpClient.newHttpClient(),
            "http://127.0.0.1:" + server.getAddress().getPort()
        );
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void get_byIdOrByUsernameIgnoringCase() {
        TEST_DB.seed(
            "update player_data set coins = 12, experience = 345 where id = '" + ALICE + "'"
        );
        var byId = players.get(ALICE.toString());
        assertNotNull(byId);
        assertEquals("Alice", byId.username());
        assertEquals(3, byId.mapSlots());
        assertEquals(1, byId.mapBuilders());
        assertEquals(0, byId.permissions());
        assertEquals(12, byId.coins());
        assertEquals(1, byId.settings().get("a").getAsInt());

        var byName = players.get("alice");
        assertNotNull(byName);
        assertEquals(ALICE, byName.id());

        assertNull(players.get("nobody"));
        assertNull(players.get("99999999-9999-9999-9999-999999999999"));
    }

    @Test
    void get_derivesTheHypercubeRoleWhileItRuns() {
        var bob = players.get(BOB.toString());
        assertNotNull(bob);
        assertEquals(Roles.EXTENDED_LIMITS, bob.permissions());
        assertEquals("hypercube/gold", bob.displayName().badge());
        assertEquals(5, bob.mapSlots());
        assertEquals(4, bob.mapBuilders());

        var carol = players.get(CAROL.toString());
        assertNotNull(carol);
        assertEquals(0, carol.permissions());
        assertNull(carol.displayName().badge());
        assertEquals(3, carol.mapBuilders());
    }

    @Test
    void displayNames_batchAndSingle() {
        var names = players.displayNames(
            List.of(ALICE, MOD, UUID.fromString("99999999-9999-9999-9999-999999999999"))
        );
        assertEquals(2, names.size());
        assertEquals("mod_1", names.get(MOD).badge());
        assertEquals(
            new DisplayName.Part.Username("Moddy", "#46fa32"),
            names.get(MOD).parts().getLast()
        );
        assertNull(names.get(ALICE).badge());
        assertEquals(
            new DisplayName.Part.Username("Alice", null),
            names.get(ALICE).parts().getFirst()
        );

        assertEquals("Moddy", players.displayName(MOD).username());
        assertNull(players.displayName(UUID.fromString("99999999-9999-9999-9999-999999999999")));
        assertTrue(players.displayNames(List.of()).isEmpty());
    }

    @Test
    void displayNames_orgAccountHasNoRow() {
        var org = UUID.fromString("b571aed9-19f4-4032-9c06-75a4b7cf6c00");
        assertEquals("Hollow Cube", players.displayName(org).username());
        assertEquals("Hollow Cube", players.displayNames(List.of(org, ALICE)).get(org).username());
    }

    @Test
    void hypercube_onlyWhileRunning() {
        assertNotNull(players.hypercube(BOB));
        assertNull(players.hypercube(CAROL));
        assertNull(players.hypercube(ALICE));
    }

    @Test
    void updateSettings_mergesAndANullRemoves() throws SQLException {
        var patch = new JsonObject();
        patch.addProperty("b", false);
        patch.add("a", JsonNull.INSTANCE);
        patch.addProperty("c", "x");
        var nested = JsonParser.parseString("{\"kept\":null,\"value\":2}").getAsJsonObject();
        patch.add("nested", nested);
        players.updateSettings(ALICE, patch);

        try (
            var st = TEST_DB.conn().createStatement();
            var rs = st.executeQuery(
                "select settings::text from player_data where id = '" + ALICE + "'"
            )
        ) {
            assertTrue(rs.next());
            var settings = JsonParser.parseString(rs.getString(1)).getAsJsonObject();
            assertFalse(settings.has("a"));
            assertFalse(settings.get("b").getAsBoolean());
            assertEquals("x", settings.get("c").getAsString());
            assertEquals(nested, settings.get("nested"));
        }

        assertEquals(nested, players.get(ALICE.toString()).settings().get("nested"));

        var missing = assertThrows(
            IpcException.class,
            () -> players.updateSettings(
                UUID.fromString("99999999-9999-9999-9999-999999999999"),
                patch
            )
        );
        assertEquals(404, missing.status());
    }

    @Test
    void search_prefixFirstAndExcludes() {
        var all = players.search("o", List.of(), 10);
        assertEquals(
            List.of("Carol", "Moddy", "bobby"),
            all.stream().map(PlayerStub::username).toList()
        );

        var prefix = players.search("bo", List.of(), 10);
        assertEquals(List.of("bobby"), prefix.stream().map(PlayerStub::username).toList());

        var excluded = players.search("o", List.of(BOB), 10);
        assertFalse(excluded.stream().anyMatch(s -> s.id().equals(BOB)));

        assertTrue(players.search("", List.of(), 10).isEmpty());
        assertEquals(1, players.search("o", List.of(), 1).size());
    }

    @Test
    void alts_areEveryoneWhoSharedAnAddress() {
        var alts = players.alts(ALICE);
        assertEquals(List.of(BOB, CAROL), alts.stream().map(PlayerStub::id).sorted().toList());
        assertEquals(List.of(ALICE), players.alts(BOB).stream().map(PlayerStub::id).toList());
        assertTrue(players.alts(MOD).isEmpty());
    }
}
