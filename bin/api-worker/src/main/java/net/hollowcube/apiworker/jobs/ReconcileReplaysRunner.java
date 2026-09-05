package net.hollowcube.apiworker.jobs;

import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.apiserver.job.CompactReplay;
import net.hollowcube.apiserver.job.JobSpec;
import net.hollowcube.apiworker.job.JobRunner;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

/// Asks for a compaction of every finished replay nothing has compacted, oldest first.
///
/// Correctness cannot rest on the enqueue in the commit transaction alone: a replay the Go server
/// finished enqueues nothing, a row can be parked, and the corpus recorded before any of this
/// existed has no row either. Left running, this is what works through that corpus.
///
/// `enqueueJob` upserts on `(job, instance)`, so asking again is free and unparks a failed row.
public final class ReconcileReplaysRunner implements JobRunner<Void> {

    /// Long enough that a pass cannot race the commit that finished the replay.
    private static final Duration LAG = Duration.ofMinutes(10);

    private static final Logger logger = LoggerFactory.getLogger(ReconcileReplaysRunner.class);

    private final ApiDatabase db;
    private final int limit;

    public ReconcileReplaysRunner(ApiDatabase db, int limit) {
        this.db = db;
        this.limit = limit;
    }

    @Override
    public void run(@Nullable Void data) {
        var ids = db.replays.listUncompactedReplays(Instant.now().minus(LAG), limit);
        if (ids.isEmpty()) return;

        for (var id : ids)
            JobSpec.COMPACT_REPLAY.enqueue(db.jobs, new CompactReplay(id, "reconcile"));
        logger.info("asked for compaction of {} finished replays", ids.size());
    }
}
