package net.hollowcube.apiserver.hdb;

import com.google.gson.Gson;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The head database end to end: a real Postgres under the service, the generated server over it,
/// and the generated client talking to that over a real socket.
///
/// Going through the wire rather than calling the service directly is the point — it is the only
/// way the route names, the JSON field names and the [HeadInfo] round trip are actually exercised.
class PostgresHeadDatabaseTest {

    @RegisterExtension
    static final TestDb TEST_DB = TestDb.of("src/main/sql/migrations");

    private HttpServer server;
    private HeadDatabaseClient hdb;

    @BeforeEach
    void start() throws IOException {
        TEST_DB.seed("""
            insert into head_db (id, category, name, tags, texture) values
                (1, 'mob', 'Creeper Head', array['green', 'scary'], 'tex-creeper'),
                (2, 'mob', 'Zombie Head', array['green'], 'tex-zombie'),
                (3, 'block', 'Stone Block', array[]::varchar[], 'tex-stone')""");

        var service = new PostgresHeadDatabase(TEST_DB.database(ApiDatabase::new));
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(HeadDatabaseServer.PATH, new HeadDatabaseServer(service, new Gson()));
        server.start();

        hdb = new HeadDatabaseClient(HttpClient.newHttpClient(), new Gson(),
            "http://127.0.0.1:" + server.getAddress().getPort());
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void getHeads_carriesEveryFieldAcrossTheWireUntouched() {
        HeadInfo creeper = hdb.getHeads("creeper", 0, 10).first();

        assertEquals("1", creeper.id());
        assertEquals("Creeper Head", creeper.name());
        assertEquals("mob", creeper.category());
        // The bare hash, not a profile: encoding it is the caller's business.
        assertEquals("tex-creeper", creeper.texture());
        assertEquals(List.of("green", "scary"), creeper.tags());
    }

    @Test
    void getHeads_matchesAPartialWordAsItIsTyped() {
        for (String typed : List.of("c", "cre", "creep", "creeper")) {
            var results = hdb.getHeads(typed, 0, 10);
            assertTrue(results.results().stream().anyMatch(head -> head.name().equals("Creeper Head")),
                "'" + typed + "' should still find the creeper");
        }
    }

    @Test
    void getHeads_matchesOnTagsToo() {
        var results = hdb.getHeads("scary", 0, 10);

        assertEquals(1, results.count());
        assertEquals("Creeper Head", results.first().name());
    }

    @Test
    void getHeads_requiresEveryWordToMatch() {
        assertEquals(1, hdb.getHeads("zombie head", 0, 10).count());
        assertEquals(0, hdb.getHeads("zombie stone", 0, 10).count());
    }

    @Test
    void getHeads_answersAnEmptyPageRatherThanEverythingWhenNothingMatches() {
        var results = hdb.getHeads("piglin", 0, 10);

        assertEquals(0, results.count());
        assertTrue(results.isEmpty());
    }

    @Test
    void getHeads_withoutAQueryBrowsesTheWholeTable() {
        for (String blank : List.of("", "   ", "!!")) {
            var results = hdb.getHeads(blank, 0, 10);
            assertEquals(3, results.count(), "'" + blank + "' is a browse, not a search");
            assertEquals(3, results.results().size());
        }
    }

    @Test
    void getHeads_countsEveryMatchNotJustThePage() {
        var page = hdb.getHeads("head", 0, 1);

        assertEquals(2, page.count());
        assertEquals(1, page.results().size());
        assertTrue(page.hasNext(0, 1));
        assertEquals(2, page.totalPages(1));
    }

    @Test
    void getHeads_walksPagesWithoutRepeatingAResult() {
        var first = hdb.getHeads("head", 0, 1).first();
        var second = hdb.getHeads("head", 1, 1).first();

        assertEquals(List.of("1", "2"), List.of(first.id(), second.id()));
    }

    @Test
    void getHeads_treatsAMissingOrOversizedPageSizeAsTheDefault() {
        assertEquals(3, hdb.getHeads("", 0, 0).results().size());
        assertEquals(3, hdb.getHeads("", 0, 5_000).results().size());
    }

    @Test
    void getHeadsInCategory_pagesOneCategoryInNameOrder() {
        var page = hdb.getHeadsInCategory("mob", 0, 1);

        assertEquals(2, page.count());
        assertEquals("Creeper Head", page.first().name());
        assertTrue(page.hasNext(0, 1));
        assertEquals("Zombie Head", hdb.getHeadsInCategory("mob", 1, 1).first().name());
    }

    @Test
    void getHeadsInCategory_isEmptyForACategoryNothingIsIn() {
        var results = hdb.getHeadsInCategory("nether", 0, 10);

        assertEquals(0, results.count());
        assertTrue(results.isEmpty());
        assertFalse(results.hasNext(0, 10));
    }
}
