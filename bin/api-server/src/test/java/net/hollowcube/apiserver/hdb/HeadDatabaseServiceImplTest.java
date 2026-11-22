package net.hollowcube.apiserver.hdb;

import com.sun.net.httpserver.HttpServer;
import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.ipc.hdb.HeadDatabaseClient;
import net.hollowcube.ipc.hdb.HeadDatabaseServer;
import net.hollowcube.ipc.hdb.HeadInfo;
import net.hollowcube.sqlgen.testing.TestDb;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The head database end to end: a real Postgres under the service, the generated server over it,
/// and the generated client talking to that over a real socket.
///
/// Going through the wire rather than calling the service directly is the point — it is the only
/// way the route names, the JSON field names and the [HeadInfo] round trip are actually exercised.
class HeadDatabaseServiceImplTest {

    @RegisterExtension
    // The schema lives with the queries in modules/api; this is the service on top of it.
    static final TestDb TEST_DB = TestDb.of("../../modules/api/src/main/sql/migrations");

    private HttpServer server;
    private HeadDatabaseClient hdb;

    @BeforeEach
    void start() throws IOException {
        TEST_DB.seed(
            """
            insert into head_db (id, category, name, tags, texture) values
                (1, 'mob', 'Creeper Head', array['green', 'scary'], 'tex-creeper'),
                (2, 'mob', 'Zombie Head', array['green'], 'tex-zombie'),
                (3, 'block', 'Stone Block', array[]::varchar[], 'tex-stone')"""
        );

        var service = new HeadDatabaseServiceImpl(TEST_DB.database(ApiDatabase::new));
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(HeadDatabaseServer.PATH, new HeadDatabaseServer(service));
        server.start();

        hdb = new HeadDatabaseClient(
            HttpClient.newHttpClient(),
            "http://127.0.0.1:" + server.getAddress().getPort()
        );
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void getHeads_matchesEveryWordAsItIsTypedAgainstNamesAndTags() {
        for (String typed : List.of("c", "cre", "creep", "creeper")) {
            var results = hdb.getHeads(typed, 0, 10);
            assertTrue(
                results.results().stream().anyMatch(head -> head.name().equals("Creeper Head")),
                "'" + typed + "' should still find the creeper"
            );
        }
        assertEquals("Creeper Head", hdb.getHeads("scary", 0, 10).first().name());
        assertEquals(1, hdb.getHeads("scary", 0, 10).count());
        assertEquals(1, hdb.getHeads("zombie head", 0, 10).count());
        assertEquals(0, hdb.getHeads("zombie stone", 0, 10).count());
        // Nothing matching is an empty page, not a fall back to browsing.
        assertTrue(hdb.getHeads("piglin", 0, 10).isEmpty());
        for (String blank : List.of("", "   ", "!!")) {
            assertEquals(3, hdb.getHeads(blank, 0, 10).count(), "'" + blank + "' is a browse");
        }
    }

    @Test
    void getHeads_pagesWithATotalAndWithoutRepeatingAResult() {
        var first = hdb.getHeads("head", 0, 1);
        assertEquals(2, first.count());
        assertEquals(1, first.results().size());
        assertTrue(first.hasNext(0, 1));
        assertEquals(
            List.of("1", "2"),
            List.of(first.first().id(), hdb.getHeads("head", 1, 1).first().id())
        );
        assertEquals(3, hdb.getHeads("", 0, 0).results().size());
        assertEquals(3, hdb.getHeads("", 0, 5_000).results().size());

        var category = hdb.getHeadsInCategory("mob", 0, 1);
        assertEquals(2, category.count());
        assertEquals("Creeper Head", category.first().name());
        assertEquals("Zombie Head", hdb.getHeadsInCategory("mob", 1, 1).first().name());
    }
}
