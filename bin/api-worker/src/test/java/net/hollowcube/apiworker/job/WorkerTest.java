package net.hollowcube.apiworker.job;

import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.apiserver.db.Jobs;
import net.hollowcube.apiserver.job.JobSpec;
import net.hollowcube.sqlgen.testing.TestDb;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/// The worker against a real `jobs` table, with runs on the calling thread so every poll is a
/// plain method call and there is nothing to wait for.
class WorkerTest {

    @RegisterExtension
    static final TestDb TEST_DB = TestDb.of("../../modules/api/src/main/sql/migrations");

    private final ApiDatabase db = TEST_DB.database(ApiDatabase::new);

    record IndexMap(String mapId, @Nullable String reason) {
    }

    private static final JobSpec<Void> COUNT = JobSpec.timed("count", "*/5 * * * *");
    private static final JobSpec<IndexMap> INDEX = JobSpec.queued("index", IndexMap.class, IndexMap::mapId).attempts(2);

    private static final class Recording<D> implements JobRunner<D> {
        final List<D> seen = new ArrayList<>();
        int runs;
        @Nullable RuntimeException failure;
        @Nullable Error death;

        @Override
        public void run(@Nullable D data) {
            runs++;
            seen.add(data);
            if (failure != null) throw failure;
            if (death != null) throw death;
        }
    }

    private Jobs row(String job, String instance) {
        return db.jobs.listJobs().stream()
            .filter(r -> r.job().equals(job) && r.instance().equals(instance))
            .findFirst().orElseThrow();
    }

    private void makeDue(String job, String instance) {
        TEST_DB.seed("update jobs set run_at = now() - interval '1 second' where job = '" + job + "' and instance = '" + instance + "'");
    }

    @Test
    void timedJob_getsItsRowOnStartRunsWhenDueAndMovesForward() {
        var runner = new Recording<Void>();
        var worker = new Worker(db, "test", 1, Runnable::run);
        worker.handle(COUNT, runner);
        worker.scheduleRecurring();

        assertEquals(0, worker.pollOnce(), "the first boundary is in the future");
        makeDue("count", "-");
        assertEquals(1, worker.pollOnce());
        assertEquals(1, runner.runs);
        assertNull(runner.seen.getFirst());

        var row = row("count", "-");
        assertNull(row.pickedBy());
        assertNotNull(row.lastSuccess());
        assertTrue(row.runAt().isAfter(Instant.now()));
        assertEquals(0, worker.pollOnce(), "not due again until its next boundary");
    }

    @Test
    void queuedJob_seesItsRecordAndIsDeletedWhenItSucceeds() {
        var runner = new Recording<IndexMap>();
        var worker = new Worker(db, "test", 1, Runnable::run);
        worker.handle(INDEX, runner);
        INDEX.enqueue(db.jobs, new IndexMap("map-1", "publish"));

        assertEquals(1, worker.pollOnce());
        assertEquals(new IndexMap("map-1", "publish"), runner.seen.getFirst());
        assertTrue(db.jobs.listJobs().isEmpty());
    }

    @Test
    void queuedJob_backsOffOnFailureAndParksAtMaxAttempts() {
        var runner = new Recording<IndexMap>();
        runner.failure = new IllegalStateException("no");
        var worker = new Worker(db, "test", 1, Runnable::run);
        worker.handle(INDEX, runner);
        INDEX.enqueue(db.jobs, new IndexMap("map-1", null));

        assertEquals(1, worker.pollOnce());
        var afterFirst = row("index", "map-1");
        assertNull(afterFirst.pickedBy());
        assertEquals(1, afterFirst.attempts());
        assertEquals("java.lang.IllegalStateException: no", afterFirst.lastError());
        assertTrue(afterFirst.runAt().isAfter(Instant.now().minusSeconds(1)), "backed off, not due now");

        makeDue("index", "map-1");
        assertEquals(1, worker.pollOnce());
        var parked = row("index", "map-1");
        assertNotNull(parked.parkedAt());
        assertEquals(2, parked.attempts());
        assertEquals(0, worker.pollOnce(), "parked rows are not picked");
    }

    @Test
    void anError_isReportedOnTheRowAndStillPropagates() {
        var runner = new Recording<IndexMap>();
        runner.death = new OutOfMemoryError("no");
        var worker = new Worker(db, "test", 1, Runnable::run);
        worker.handle(INDEX, runner);
        INDEX.enqueue(db.jobs, new IndexMap("map-1", null));

        assertThrows(OutOfMemoryError.class, worker::pollOnce);
        var row = row("index", "map-1");
        assertNull(row.pickedBy(), "handed back, not left for the reaper");
        assertEquals(1, row.attempts());
        assertEquals("java.lang.OutOfMemoryError: no", row.lastError());
        assertEquals(0, worker.pollOnce(), "backed off, and the slot came back");
    }

    @Test
    void undecodableData_isParkedAtOnce() {
        var runner = new Recording<IndexMap>();
        var worker = new Worker(db, "test", 1, Runnable::run);
        worker.handle(INDEX, runner);
        TEST_DB.seed("insert into jobs (job, instance, data) values ('index', 'bad', '[1, 2]')");

        assertEquals(1, worker.pollOnce());
        assertEquals(0, runner.runs);
        var row = row("index", "bad");
        assertNotNull(row.parkedAt());
        assertTrue(row.lastError().startsWith("data is not a IndexMap"), row.lastError());
    }

    @Test
    void missingData_isParkedAtOnce() {
        var runner = new Recording<IndexMap>();
        var worker = new Worker(db, "test", 1, Runnable::run);
        worker.handle(INDEX, runner);
        TEST_DB.seed("insert into jobs (job, instance) values ('index', 'bare')");

        assertEquals(1, worker.pollOnce());
        assertEquals(0, runner.runs);
        var row = row("index", "bare");
        assertNotNull(row.parkedAt());
        assertTrue(row.lastError().startsWith("data is missing"), row.lastError());
    }

    @Test
    void timedJob_goesAheadOfTheQueue() {
        var count = new Recording<Void>();
        var index = new Recording<IndexMap>();
        var pending = new ArrayList<Runnable>();
        var worker = new Worker(db, "test", 1, pending::add);
        worker.handle(INDEX, index);
        worker.handle(COUNT, count);
        for (int i = 0; i < 3; i++) INDEX.enqueue(db.jobs, new IndexMap("map-" + i, null));
        worker.scheduleRecurring();
        makeDue("count", "-");

        assertEquals(1, worker.pollOnce());
        pending.removeFirst().run();
        assertEquals(1, count.runs, "the one slot went to the timed job, not the older queue");
        assertEquals(0, index.runs);
    }

    @Test
    void slots_boundWhatOnePollPicks() {
        var runner = new Recording<IndexMap>();
        var pending = new ArrayList<Runnable>();
        var worker = new Worker(db, "test", 1, pending::add);
        worker.handle(INDEX, runner);
        INDEX.enqueue(db.jobs, new IndexMap("map-1", null));
        INDEX.enqueue(db.jobs, new IndexMap("map-2", null));

        assertEquals(1, worker.pollOnce(), "one slot, one row");
        assertEquals(0, worker.pollOnce(), "the slot is held until the run finishes");
        assertEquals(1, db.jobs.listJobs().stream().filter(r -> r.pickedBy() == null).count());

        pending.removeFirst().run();
        assertEquals(1, worker.pollOnce());
        pending.removeFirst().run();
        assertEquals(2, runner.runs);
        assertTrue(db.jobs.listJobs().isEmpty());
    }

    @Test
    void queuedJob_reEnqueuedWhileRunning_staysDueInsteadOfBeingDeleted() {
        var runner = new Recording<IndexMap>();
        var pending = new ArrayList<Runnable>();
        var worker = new Worker(db, "test", 1, pending::add);
        worker.handle(INDEX, runner);
        INDEX.enqueue(db.jobs, new IndexMap("map-1", null));

        assertEquals(1, worker.pollOnce());
        INDEX.enqueue(db.jobs, new IndexMap("map-1", "again"));
        pending.removeFirst().run();

        var row = row("index", "map-1");
        assertNull(row.pickedBy());
        assertEquals(1, worker.pollOnce(), "and it runs again");
        pending.removeFirst().run();
        assertEquals("again", runner.seen.getLast().reason(), "with the data it was re-enqueued with");
    }

    @Test
    void backoff_growsWithTheFourthPowerAndCaps() {
        assertEquals(Duration.ofSeconds(1), Worker.backoff(1));
        assertEquals(Duration.ofSeconds(16), Worker.backoff(2));
        assertEquals(Duration.ofSeconds(625), Worker.backoff(5));
        assertEquals(Duration.ofHours(1), Worker.backoff(20));
    }
}
