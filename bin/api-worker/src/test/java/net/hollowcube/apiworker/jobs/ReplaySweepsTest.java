package net.hollowcube.apiworker.jobs;

import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.apiserver.db.Jobs;
import net.hollowcube.apiserver.job.JobSpec;
import net.hollowcube.ipc.Blob;
import net.hollowcube.ipc.replay.ReplayCommit;
import net.hollowcube.ipc.replay.ReplayCompaction;
import net.hollowcube.ipc.replay.ReplayInfo;
import net.hollowcube.ipc.replay.ReplayService;
import net.hollowcube.ipc.util.IpcException;
import net.hollowcube.sqlgen.testing.TestDb;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The two timed jobs that stand behind the per-commit enqueue: the one that finds the replays
/// nothing asked to compact, and the one that removes what compaction made redundant.
class ReplaySweepsTest {

    @RegisterExtension
    static final TestDb TEST_DB = TestDb.of("../../modules/api/src/main/sql/migrations");

    private final ApiDatabase db = TEST_DB.database(ApiDatabase::new);

    @Test
    void reconcile_asksForEveryFinishedReplayNothingHasCompacted() throws Exception {
        replay("old-finished", "finished", "segmented", Duration.ofHours(2));
        replay("still-recording", "recording", "segmented", Duration.ofHours(2));
        replay("already-compacted", "finished", "compacted", Duration.ofHours(2));
        // Inside the lag, so the commit that finished it may still be enqueueing its own row.
        replay("just-finished", "finished", "segmented", Duration.ofMinutes(1));

        new ReconcileReplaysRunner(db, 100).run(null);

        assertEquals(List.of("old-finished"), enqueued());
    }

    @Test
    void reconcile_asksAgainForOneWhoseRowWasParked() throws Exception {
        replay("old-finished", "finished", "segmented", Duration.ofHours(2));
        new ReconcileReplaysRunner(db, 100).run(null);
        TEST_DB.seed("update jobs set parked_at = now(), attempts = 5 where job = 'compact-replay'");

        new ReconcileReplaysRunner(db, 100).run(null);

        assertEquals(List.of("old-finished"), enqueued());
        var row = db.jobs.listJobs().getFirst();
        assertNull(row.parkedAt());
        assertEquals(0, row.attempts());
    }

    @Test
    void sweep_withDeletingOffTouchesNothing() throws Exception {
        replay("compacted", "finished", "compacted", Duration.ofDays(2));
        segment("compacted", 0, "replays/ab/segments/0/cd");
        var replays = new FakeReplayService();

        new SweepReplaySourcesRunner(db, replays, 100, false).run(null);

        assertEquals(List.of(), replays.dropped);
        assertEquals(1, db.replays.listReplaySegmentObjects("compacted").size());
    }

    @Test
    void sweep_dropsTheSourcesOfCompactedReplaysPastTheGrace() throws Exception {
        replay("compacted", "finished", "compacted", Duration.ofDays(2));
        segment("compacted", 0, "replays/ab/segments/0/cd");
        // Compacted, but only just: the grace is what protects a compaction that raced the sweep.
        replay("fresh", "finished", "compacted", Duration.ofHours(1));
        segment("fresh", 0, "replays/ef/segments/0/gh");
        // Still segmented, so its segments are the replay.
        replay("segmented", "finished", "segmented", Duration.ofDays(2));
        segment("segmented", 0, "replays/ij/segments/0/kl");
        var replays = new FakeReplayService();

        new SweepReplaySourcesRunner(db, replays, 100, true).run(null);

        assertEquals(List.of("compacted"), replays.dropped);
    }

    @Test
    void sweep_expiresIdempotencyRecordsOlderThanAWeek() throws Exception {
        replay("compacted", "finished", "compacted", Duration.ofDays(2));
        idempotency("compacted", "old", Duration.ofDays(8));
        idempotency("compacted", "recent", Duration.ofHours(1));

        new SweepReplaySourcesRunner(db, new FakeReplayService(), 100, false).run(null);

        assertEquals(null, db.replays.getReplayIdempotency("compacted", "old"));
        assertNotNull(db.replays.getReplayIdempotency("compacted", "recent"));
    }

    private List<String> enqueued() {
        return db.jobs.listJobs().stream()
            .filter(job -> job.job().equals(JobSpec.COMPACT_REPLAY.name()))
            .map(Jobs::instance)
            .sorted()
            .toList();
    }

    private void replay(String id, String state, String representation, Duration ago) {
        var compacted = representation.equals("compacted");
        TEST_DB.seed("""
            insert into replays (id, version, recording_revision, state, representation, next_segment_index,
                                 current_preamble, current_preamble_digest,
                                 compacted_source_revision, compacted_object, compacted_length, compacted_digest,
                                 updated_at)
            values ('%s', 2, 2, '%s', '%s', 1, '\\x00', decode(repeat('11', 32), 'hex'),
                    %s, %s, %s, %s, now() - interval '%d seconds')
            """.formatted(id, state, representation,
            compacted ? "2" : "null",
            compacted ? "'replays/aa/compacted/bb'" : "null",
            compacted ? "16" : "null",
            compacted ? "decode(repeat('22', 32), 'hex')" : "null",
            ago.toSeconds()));
    }

    private void segment(String replayId, int index, String object) {
        TEST_DB.seed("""
            insert into replay_segments (replay_id, segment_index, object_reference, length, digest, commit_revision)
            values ('%s', %d, '%s', 8, decode(repeat('33', 32), 'hex'), 1)
            """.formatted(replayId, index, object));
    }

    private void idempotency(String replayId, String key, Duration ago) {
        TEST_DB.seed("""
            insert into replay_idempotency (replay_id, idempotency_key, request_fingerprint, response_status,
                                            response_etag, response_metadata, created_at)
            values ('%s', '%s', decode(repeat('44', 32), 'hex'), 200, '"r2"', '{}',
                    now() - interval '%d seconds')
            """.formatted(replayId, key, ago.toSeconds()));
    }

    /// Storage that only remembers which replays it was asked to drop the sources of. Everything
    /// else asserts, because a sweep that reads a replay is a sweep doing something it should not.
    private static final class FakeReplayService implements ReplayService {
        private final List<String> dropped = new ArrayList<>();

        @Override
        public int dropSegments(String id) {
            dropped.add(id);
            return 1;
        }

        @Override
        public @Nullable ReplayInfo getReplay(String id) {
            throw new AssertionError("the sweeper only drops segments");
        }

        @Override
        public Blob getPreamble(String id, @Nullable Long expectedRevision) {
            throw new AssertionError("the sweeper only drops segments");
        }

        @Override
        public Blob getSegment(String id, int segmentIndex) {
            throw new AssertionError("the sweeper only drops segments");
        }

        @Override
        public Blob getCompacted(String id, @Nullable Long start, @Nullable Long endInclusive) {
            throw new AssertionError("the sweeper only drops segments");
        }

        @Override
        public ReplayInfo commit(ReplayCommit meta, Blob body) {
            throw new AssertionError("the sweeper only drops segments");
        }

        @Override
        public ReplayInfo publishCompacted(ReplayCompaction meta, Blob body) {
            throw new AssertionError("the sweeper only drops segments");
        }
    }
}
