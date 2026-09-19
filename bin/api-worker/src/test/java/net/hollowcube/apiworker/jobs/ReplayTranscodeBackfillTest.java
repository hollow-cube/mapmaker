package net.hollowcube.apiworker.jobs;

import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.apiserver.db.Jobs;
import net.hollowcube.apiserver.job.JobSpec;
import net.hollowcube.apiserver.job.SampleReplayTranscode;
import net.hollowcube.sqlgen.testing.TestDb;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Duration;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReplayTranscodeBackfillTest {

    @RegisterExtension
    static final TestDb TEST_DB = TestDb.of("../../modules/api/src/main/sql/migrations");

    private static final Duration IDLE = Duration.ofDays(7);

    private final ApiDatabase db = TEST_DB.database(ApiDatabase::new);

    @Test
    void backfill_topsTheQueueUpFromFormat4ReplaysNothingHasAskedFor() throws Exception {
        replay("a", "finished", 4);
        replay("b", "finished", 4);
        transcodeRow("b", false);
        // Failed five times: left for a human rather than offered again.
        replay("c", "finished", 4);
        transcodeRow("c", true);
        replay("d", "finished", 5);
        // Not idle yet.
        replay("e", "recording", 4);
        replay("f", "finished", 3);
        replay("g", "finished", 4);
        replay("h", "finished", 4);
        replay("i", "recording", 4);
        TEST_DB.seed("update replays set updated_at = now() - interval '30 days' where id = 'i'");

        // One waiting already, so room for two.
        new BackfillReplayTranscodeRunner(db, 3, IDLE).run(null);

        assertEquals(List.of("a", "b", "c", "g"), transcodeRows());
        assertEquals(List.of("c"), parkedRows());

        new BackfillReplayTranscodeRunner(db, 3, IDLE).run(null);
        assertEquals(List.of("a", "b", "c", "g"), transcodeRows());

        TEST_DB.seed("delete from jobs where instance in ('a', 'b')");
        TEST_DB.seed(
            "update replays set current_preamble = '\\x484352500005' where id in ('a', 'b')"
        );
        new BackfillReplayTranscodeRunner(db, 3, IDLE).run(null);
        assertEquals(List.of("c", "g", "h", "i"), transcodeRows());
    }

    @Test
    void sample_transcodesWithoutWritingAnything() throws Exception {
        var compacted = LegacyReplays.compacted();
        var storage = TranscodeReplayRunnerTest.FakeStorage.compacted(compacted);
        TEST_DB.seed(
            """
            insert into replays (id, version, recording_revision, state, representation, next_segment_index,
                                 current_preamble, current_preamble_digest,
                                 compacted_source_revision, compacted_object, compacted_length, compacted_digest)
            values ('%s', 3, 2, 'finished', 'compacted', 1, decode('%s', 'hex'), decode(repeat('11', 32), 'hex'),
                    2, 'replays/aa/compacted/bb', %d, decode(repeat('22', 32), 'hex'))
            """
                .formatted(
                    TranscodeReplayRunnerTest.ID,
                    HexFormat.of().formatHex(Arrays.copyOf(compacted, 300)),
                    compacted.length
                )
        );
        var before = db.replays.getReplay(TranscodeReplayRunnerTest.ID);

        new SampleReplayTranscodeRunner(db, storage, IDLE).run(new SampleReplayTranscode(10, null));

        assertEquals(List.of(), storage.published);
        assertEquals(List.of(), db.jobs.listJobs());
        var after = db.replays.getReplay(TranscodeReplayRunnerTest.ID);
        assertEquals(before.version(), after.version());
        assertEquals(before.compactedObject(), after.compactedObject());
    }

    private List<String> transcodeRows() {
        return db.jobs.listJobs()
            .stream()
            .filter(job -> job.job().equals(JobSpec.TRANSCODE_REPLAY.name()))
            .map(Jobs::instance)
            .sorted()
            .toList();
    }

    private List<String> parkedRows() {
        return db.jobs.listJobs()
            .stream()
            .filter(job -> job.parkedAt() != null)
            .map(Jobs::instance)
            .toList();
    }

    private void replay(String id, String state, int version) {
        TEST_DB.seed(
            """
            insert into replays (id, version, recording_revision, state, representation, next_segment_index,
                                 current_preamble, current_preamble_digest)
            values ('%s', 2, 2, '%s', 'segmented', 1, '\\x4843525000%02x', decode(repeat('11', 32), 'hex'))
            """
                .formatted(id, state, version)
        );
    }

    private void transcodeRow(String id, boolean parked) {
        TEST_DB.seed(
            """
            insert into jobs (job, instance, data, parked_at)
            values ('transcode-replay', '%s', '{"replayId": "%s"}', %s)
            """
                .formatted(id, id, parked ? "now()" : "null")
        );
    }
}
