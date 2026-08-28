package net.hollowcube.apiserver.db;

import net.hollowcube.sqlgen.testing.TestDb;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/// Drives the `jobs` queries against a real Postgres: what makes a row due, who gets it, and what
/// each way of letting go of it leaves behind. Everything here runs inside one transaction, so
/// `now()` is the same instant throughout a test and the seeds lean on that.
class JobsQueriesTest {

    @RegisterExtension
    static final TestDb TEST_DB = TestDb.of("src/main/sql/migrations");

    private final ApiDatabase db = TEST_DB.database(ApiDatabase::new);

    private void due(String job, String instance) {
        TEST_DB.seed("insert into jobs (job, instance, run_at) values ('" + job + "', '" + instance + "', now() - interval '1 second')");
    }

    private Jobs row(String job, String instance) {
        return db.jobs.listJobs().stream()
            .filter(r -> r.job().equals(job) && r.instance().equals(instance))
            .findFirst().orElseThrow();
    }

    @Test
    void scheduleJob_keepsTheRowThatIsAlreadyThere() {
        var first = Instant.parse("2026-08-29T12:05:00Z");
        db.jobs.scheduleJob("count", "-", first);
        db.jobs.scheduleJob("count", "-", Instant.parse("2026-08-29T12:10:00Z"));

        assertEquals(first, row("count", "-").runAt());
    }

    @Test
    void pickJobs_takesDueRowsOfTheNamedJobsUpToTheLimitAndNeverTwice() {
        due("index", "a");
        due("index", "b");
        due("index", "c");
        due("other", "d");
        db.jobs.scheduleJob("index", "later", Instant.now().plusSeconds(3600));

        var first = db.jobs.pickJobs("w1", List.of("index"), 2);
        assertEquals(List.of("a", "b"), first.stream().map(Jobs::instance).toList());
        assertTrue(first.stream().allMatch(r -> "w1".equals(r.pickedBy()) && r.pickedAt() != null && r.heartbeat() != null));

        var second = db.jobs.pickJobs("w2", List.of("index"), 5);
        assertEquals(List.of("c"), second.stream().map(Jobs::instance).toList());

        assertTrue(db.jobs.pickJobs("w2", List.of("index"), 5).isEmpty());
        assertNull(row("other", "d").pickedBy());
    }

    @Test
    void pickJobs_takesTimedRowsBeforeTheQueueAndSkipsParkedOnes() {
        db.jobs.enqueueJob("index", "queued", null);
        db.jobs.enqueueJob("index", "parked", null);
        TEST_DB.seed("update jobs set parked_at = now() where instance = 'parked'");
        due("count", "-");

        var picked = db.jobs.pickJobs("w1", List.of("index", "count"), 5);
        assertEquals(List.of("count", "index"), picked.stream().map(Jobs::job).toList());
        assertEquals("queued", picked.getLast().instance());
    }

    @Test
    void enqueueJob_makesAnExistingRowDueAgainAndBumpsItsVersion() {
        db.jobs.enqueueJob("index", "m", null);
        TEST_DB.seed("update jobs set run_at = now() + interval '1 hour', parked_at = now(), attempts = 5 where instance = 'm'");

        db.jobs.enqueueJob("index", "m", "{\"reason\": \"publish\"}");

        var row = row("index", "m");
        assertNull(row.runAt(), "the backoff is dropped");
        assertNull(row.parkedAt());
        assertEquals(0, row.attempts(), "and so are the failures that parked it");
        assertEquals(1, row.version());
        assertEquals("{\"reason\": \"publish\"}", row.data());

        db.jobs.enqueueJob("index", "m", null);
        assertEquals("{\"reason\": \"publish\"}", row("index", "m").data(), "no new data keeps the old");
    }

    @Test
    void finishJob_leavesARowThatWasReEnqueuedWhileItRan() {
        due("index", "m");
        var picked = db.jobs.pickJobs("w1", List.of("index"), 1).getFirst();
        db.jobs.enqueueJob("index", "m", null);

        assertEquals(0, db.jobs.finishJob("index", "m", "w1", picked.pickedAt(), picked.version()));
        assertEquals(1, db.jobs.releaseJob("index", "m", "w1", picked.pickedAt()));

        var row = row("index", "m");
        assertNull(row.pickedBy());
        assertNull(row.parkedAt());
        assertNotNull(row.lastSuccess());
    }

    @Test
    void finishJob_deletesARowNobodyTouched() {
        due("index", "m");
        var picked = db.jobs.pickJobs("w1", List.of("index"), 1).getFirst();

        assertEquals(1, db.jobs.finishJob("index", "m", "w1", picked.pickedAt(), picked.version()));
        assertTrue(db.jobs.listJobs().isEmpty());
    }

    @Test
    void completeFailAndPark_releaseTheRowInTheirOwnWays() {
        due("count", "-");
        var picked = db.jobs.pickJobs("w1", List.of("count"), 1).getFirst();
        var next = Instant.now().plusSeconds(3600).truncatedTo(ChronoUnit.MILLIS);
        db.jobs.completeJob(next, "count", "-", "w1", picked.pickedAt());
        var completed = row("count", "-");
        assertNull(completed.pickedBy());
        assertNull(completed.pickedAt());
        assertEquals(next, completed.runAt());
        assertNotNull(completed.lastSuccess());

        assertTrue(db.jobs.pickJobs("w1", List.of("count"), 1).isEmpty(), "not due until then");
        TEST_DB.seed("update jobs set run_at = now() - interval '1 second' where job = 'count'");
        picked = db.jobs.pickJobs("w1", List.of("count"), 1).getFirst();
        db.jobs.failJob(new JobsQueries.FailJobParams(next, "boom", "count", "-", "w1", picked.pickedAt()));
        var failed = row("count", "-");
        assertNull(failed.pickedBy());
        assertEquals(1, failed.attempts());
        assertEquals("boom", failed.lastError());

        TEST_DB.seed("update jobs set run_at = now() - interval '1 second' where job = 'count'");
        picked = db.jobs.pickJobs("w1", List.of("count"), 1).getFirst();
        db.jobs.parkJob("boom again", "count", "-", "w1", picked.pickedAt());
        var parked = row("count", "-");
        assertNotNull(parked.parkedAt());
        assertEquals(2, parked.attempts());
    }

    @Test
    void releases_onlyTouchTheHolderOfThatPick() {
        due("count", "-");
        var picked = db.jobs.pickJobs("w1", List.of("count"), 1).getFirst();

        assertEquals(0, db.jobs.completeJob(Instant.now(), "count", "-", "w2", picked.pickedAt()));
        assertEquals(0, db.jobs.yieldJob("count", "-", "w2", picked.pickedAt()));
        assertEquals("w1", row("count", "-").pickedBy());
    }

    @Test
    void aRunWhoseRowWasRevivedAndPickedAgainNoLongerSpeaksForIt() {
        due("index", "m");
        var stale = db.jobs.pickJobs("w1", List.of("index"), 1).getFirst();
        TEST_DB.seed("update jobs set heartbeat = now() - interval '1 hour' where instance = 'm'");
        assertEquals(1, db.jobs.reviveDeadJobs(40).size());
        // The same replica picks it up again: same holder, different pick.
        var fresh = db.jobs.pickJobs("w1", List.of("index"), 1).getFirst();
        assertNotEquals(stale.pickedAt(), fresh.pickedAt());

        assertEquals(0, db.jobs.finishJob("index", "m", "w1", stale.pickedAt(), stale.version()));
        assertEquals(0, db.jobs.releaseJob("index", "m", "w1", stale.pickedAt()));
        assertEquals("w1", row("index", "m").pickedBy(), "the fresh run still holds it");
        assertEquals(1, db.jobs.finishJob("index", "m", "w1", fresh.pickedAt(), fresh.version()));
    }

    @Test
    void heartbeatJobs_touchesEverythingTheHolderHasAndNothingElse() {
        due("index", "mine");
        due("index", "theirs");
        db.jobs.pickJobs("w1", List.of("index"), 1);
        db.jobs.pickJobs("w2", List.of("index"), 1);
        TEST_DB.seed("update jobs set heartbeat = now() - interval '1 hour'");

        assertEquals(1, db.jobs.heartbeatJobs("w1"));
        assertEquals(List.of("theirs"), db.jobs.reviveDeadJobs(40).stream().map(Jobs::instance).toList());
    }

    @Test
    void reviveDeadJobs_putsBackRowsWhoseHolderStoppedHeartbeating() {
        TEST_DB.seed("""
            insert into jobs (job, instance, run_at, picked_by, heartbeat) values
                ('index', 'dead', now() - interval '1 hour', 'w1', now() - interval '1 hour'),
                ('index', 'alive', now() - interval '1 hour', 'w1', now())""");

        var revived = db.jobs.reviveDeadJobs(40);
        assertEquals(List.of("dead"), revived.stream().map(Jobs::instance).toList());

        var dead = row("index", "dead");
        assertNull(dead.pickedBy());
        assertEquals(1, dead.attempts());
        assertEquals("lost by w1", dead.lastError());
        assertEquals("w1", row("index", "alive").pickedBy());
    }
}
