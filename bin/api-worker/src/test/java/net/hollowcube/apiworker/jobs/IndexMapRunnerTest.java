package net.hollowcube.apiworker.jobs;

import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.apiserver.job.IndexMap;
import net.hollowcube.apiworker.index.MapIndexer;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.api.maps.MapClient;
import net.hollowcube.sqlgen.testing.TestDb;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.net.ssl.SSLSession;

import static org.junit.jupiter.api.Assertions.*;

class IndexMapRunnerTest {

    @RegisterExtension
    static final TestDb TEST_DB = TestDb.of("../../modules/api/src/main/sql/migrations");

    private static final UUID MAP = UUID.fromString("0eb050e4-6bc3-403e-aef0-d195227f55c3");

    private final ApiDatabase db = TEST_DB.database(ApiDatabase::new);

    /// A map client that has the fixture and nothing else.
    private static final MapClient MAPS = new MapClient() {
        @Override
        public byte[] getWorld(String mapId) {
            if (!MAP.toString().equals(mapId)) throw new ApiClient.NotFoundError(new NotFound());
            try (var in = IndexMapRunnerTest.class.getResourceAsStream("/index/super-fun-map.polar")) {
                assertNotNull(in);
                return in.readAllBytes();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    };

    @Test
    void writesTheRow_andReplacesItWhenRunAgain() {
        var runner = new IndexMapRunner(db, MAPS);
        runner.run(new IndexMap(MAP.toString(), "test"));

        var row = db.mapFeatures.getMapFeatures(MAP);
        assertNotNull(row);
        assertEquals(MapIndexer.FEATURE_VERSION, row.featureVersion());
        assertNotNull(row.dataVersion());
        assertEquals(90, row.blockCount());
        assertEquals(3, row.checkpointCount());
        assertEquals(19.235, row.checkpointSpacing(), 1e-3);
        assertEquals(List.of("blocks", "reset_height"), row.mechanics(), "lower-cased, in enum order");
        assertEquals(List.of("scale"), row.attributes());
        assertEquals(List.of("reset_in_water"), row.settings());
        assertEquals(0, row.decodeFailures());

        runner.run(new IndexMap(MAP.toString(), "again"));
        var again = db.mapFeatures.getMapFeatures(MAP);
        assertNotNull(again);
        assertEquals(row.blockCount(), again.blockCount());
        assertFalse(again.indexedAt().isBefore(row.indexedAt()));
    }

    @Test
    void missingMap_isDroppedWithoutARow() {
        var gone = UUID.randomUUID();
        new IndexMapRunner(db, MAPS).run(new IndexMap(gone.toString(), null));
        assertNull(db.mapFeatures.getMapFeatures(gone));
    }

    @Test
    void nullData_isRefused() {
        assertThrows(IllegalArgumentException.class, () -> new IndexMapRunner(db, MAPS).run(null));
    }

    /// The api client's errors are built from a response, of which only the status is read.
    private static final class NotFound implements HttpResponse<String> {
        public int statusCode() { return 404; }
        public HttpRequest request() { return null; }
        public Optional<HttpResponse<String>> previousResponse() { return Optional.empty(); }
        public HttpHeaders headers() { return HttpHeaders.of(Map.of(), (a, b) -> true); }
        public String body() { return ""; }
        public Optional<SSLSession> sslSession() { return Optional.empty(); }
        public URI uri() { return null; }
        public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
    }
}
