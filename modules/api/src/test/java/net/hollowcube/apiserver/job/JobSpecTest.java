package net.hollowcube.apiserver.job;

import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.sqlgen.testing.TestDb;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.*;

class JobSpecTest {

    @RegisterExtension
    static final TestDb TEST_DB = TestDb.of("src/main/sql/migrations");

    private final ApiDatabase db = TEST_DB.database(ApiDatabase::new);

    record IndexMap(String mapId, String reason) {
    }

    private static final JobSpec<IndexMap> INDEX_MAP = JobSpec.queued("index-map", IndexMap.class, IndexMap::mapId).attempts(3);

    @Test
    void enqueue_keysTheRowByTheDataAndStoresTheDataAsJson() {
        INDEX_MAP.enqueue(db.jobs, new IndexMap("abc", "publish"));

        var row = db.jobs.listJobs().getFirst();
        assertEquals("index-map", row.job());
        assertEquals("abc", row.instance());
        assertEquals(new IndexMap("abc", "publish"), INDEX_MAP.decode(row.data()));
        assertEquals(3, INDEX_MAP.maxAttempts());
    }

    @Test
    void aTimedSpec_hasNothingToEnqueue() {
        assertEquals("-", JobSpec.PLAYER_COUNT.instance().apply(null));
        assertNull(JobSpec.PLAYER_COUNT.decode(null));
        assertThrows(IllegalArgumentException.class, () -> JobSpec.PLAYER_COUNT.enqueue(db.jobs, null));
    }
}
