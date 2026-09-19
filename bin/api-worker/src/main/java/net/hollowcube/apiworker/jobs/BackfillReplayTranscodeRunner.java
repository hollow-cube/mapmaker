package net.hollowcube.apiworker.jobs;

import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.apiserver.job.JobSpec;
import net.hollowcube.apiserver.job.TranscodeReplay;
import net.hollowcube.apiworker.job.JobRunner;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

/// Drives the one-off rewrite of every replay still on format 4, by keeping up to `batch`
/// [JobSpec#TRANSCODE_REPLAY] rows waiting until the scan comes up empty. Replays that already have
/// a row, including parked ones, are not offered again.
///
/// Only bound when `replay.transcode.backfill` is set; unbinding it stops the backfill with
/// whatever is queued still draining.
public final class BackfillReplayTranscodeRunner implements JobRunner<Void> {

    private static final Logger logger = LoggerFactory.getLogger(
        BackfillReplayTranscodeRunner.class
    );

    private final ApiDatabase db;
    private final int batch;
    private final Duration recordingIdle;

    public BackfillReplayTranscodeRunner(ApiDatabase db, int batch, Duration recordingIdle) {
        this.db = db;
        this.batch = batch;
        this.recordingIdle = recordingIdle;
    }

    @Override
    public void run(@Nullable Void data) {
        var job = JobSpec.TRANSCODE_REPLAY.name();
        var waiting = db.jobs.countWaitingJobs(job);
        var room = batch - (waiting == null ? 0 : waiting);
        if (room <= 0) return;

        var ids = db.replays.listLegacyFormatReplays(
            Instant.now().minus(recordingIdle),
            "",
            job,
            room
        );
        for (var id : ids)
            JobSpec.TRANSCODE_REPLAY.enqueue(db.jobs, new TranscodeReplay(id, "backfill"));
        if (!ids.isEmpty())
            logger.info("asked for rewrites of {} replays still on format 4", ids.size());
    }
}
