package net.hollowcube.apiserver.session;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import net.hollowcube.apiserver.db.ApiDatabase;
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

/// Sessions end to end, the same way as the head database: a real Postgres under the service, the
/// generated server over it, and the generated client talking to that over a real socket.
class PostgresSessionsTest {

    @RegisterExtension
    static final TestDb TEST_DB = TestDb.of("../../modules/api/src/main/sql/migrations");

    private HttpServer server;
    private SessionClient sessions;

    @BeforeEach
    void start() throws IOException {
        var service = new PostgresSessions(TEST_DB.database(ApiDatabase::new));
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(SessionServer.PATH, new SessionServer(service, new Gson()));
        server.start();

        sessions = new SessionClient(HttpClient.newHttpClient(), new Gson(),
            "http://127.0.0.1:" + server.getAddress().getPort());
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
        TEST_DB.seed("""
            insert into player_sessions (player_id, proxy_id, skin_texture, skin_signature, hidden) values
                ('11111111-1111-1111-1111-111111111111', 'proxy-a', '', '', false),
                ('22222222-2222-2222-2222-222222222222', 'proxy-b', '', '', false),
                ('33333333-3333-3333-3333-333333333333', 'proxy-a', '', '', true)""");

        assertEquals(3, sessions.onlinePlayers());
    }
}
