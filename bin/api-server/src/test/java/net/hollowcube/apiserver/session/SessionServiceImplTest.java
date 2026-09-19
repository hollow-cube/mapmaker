package net.hollowcube.apiserver.session;

import com.sun.net.httpserver.HttpServer;
import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.ipc.session.GameServer;
import net.hollowcube.ipc.session.SessionClient;
import net.hollowcube.ipc.session.SessionServer;
import net.hollowcube.sqlgen.testing.TestDb;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/// Sessions end to end, the same way as the head database: a real Postgres under the service, the
/// generated server over it, and the generated client talking to that over a real socket.
class SessionServiceImplTest {

    @RegisterExtension
    static final TestDb TEST_DB = TestDb.of("../../modules/api/src/main/sql/migrations");

    private HttpServer server;
    private SessionClient sessions;

    @BeforeEach
    void start() throws IOException {
        var service = new SessionServiceImpl(TEST_DB.database(ApiDatabase::new));
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(SessionServer.PATH, new SessionServer(service));
        server.start();

        sessions = new SessionClient(
            HttpClient.newHttpClient(),
            "http://127.0.0.1:" + server.getAddress().getPort()
        );
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void onlinePlayers_isZeroWithNobodyOn() {
        assertEquals(0, sessions.onlinePlayers());
    }

    @Test
    void onlinePlayers_countsEveryRowHiddenOrNot() {
        TEST_DB.seed(
            """
            insert into player_sessions (player_id, proxy_id, skin_texture, skin_signature, hidden) values
                ('11111111-1111-1111-1111-111111111111', 'proxy-a', '', '', false),
                ('22222222-2222-2222-2222-222222222222', 'proxy-b', '', '', false),
                ('33333333-3333-3333-3333-333333333333', 'proxy-a', '', '', true)"""
        );

        assertEquals(3, sessions.onlinePlayers());
    }

    @Test
    void findHub_returnsAReadyHubWithItsProtocol() {
        TEST_DB.seed(
            """
            insert into server_states (id, role, status, cluster_ip, protocol_version) values
                ('hub-starting', 'hub', 0, '10.0.0.1', 0),
                ('map-a', 'map', 1, '10.0.0.2', 777),
                ('hub-a', 'hub', 1, '10.0.0.3', 777)"""
        );

        assertEquals(new GameServer("hub-a", "10.0.0.3", 777), sessions.findHub(null));
    }

    @Test
    void findHub_skipsTheExcludedHub() {
        TEST_DB.seed(
            """
            insert into server_states (id, role, status, cluster_ip) values
                ('hub-a', 'hub', 1, '10.0.0.3')"""
        );

        assertNull(sessions.findHub("hub-a"));
        assertEquals(new GameServer("hub-a", "10.0.0.3", 0), sessions.findHub("hub-b"));
    }

    @Test
    void findServer_isNullOnceThePodIsGone() {
        TEST_DB.seed(
            """
            insert into server_states (id, role, status, cluster_ip, protocol_version) values
                ('isolate-a', 'map-isolate', 0, '10.0.0.4', 777),
                ('isolate-b', 'map-isolate', 0, '', 0)"""
        );

        assertEquals(
            new GameServer("isolate-a", "10.0.0.4", 777),
            sessions.findServer("isolate-a")
        );
        assertNull(sessions.findServer("isolate-b"));
        assertNull(sessions.findServer("isolate-c"));
    }
}
