package net.hollowcube.apiworker.jobs;

import net.hollowcube.apiserver.job.SweepReplayObjects;
import net.hollowcube.apiworker.job.JobRunner;
import net.hollowcube.ipc.replay.ReplayService;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Sweeps one shard of the replay bucket through the api-server, which holds the credentials.
///
/// [#deleting] starts off for the same reason as [SweepReplaySourcesRunner]'s: a dry pass over a
/// few shards is read in the logs first.
public final class SweepReplayObjectsRunner implements JobRunner<SweepReplayObjects> {

    private static final Logger logger = LoggerFactory.getLogger(SweepReplayObjectsRunner.class);

    private final ReplayService replays;
    private final boolean deleting;

    public SweepReplayObjectsRunner(ReplayService replays, boolean deleting) {
        this.replays = replays;
        this.deleting = deleting;
    }

    @Override
    public void run(@Nullable SweepReplayObjects data) {
        if (data == null)
            throw new IllegalArgumentException(
                "sweep-replay-objects needs data: {\"shard\": \"0a1b\"}"
            );
        var orphans = replays.sweepOrphanedObjects(data.shard(), deleting);
        if (orphans > 0)
            logger.info(
                "shard {} held {} orphaned replay objects{}",
                data.shard(),
                orphans,
                deleting ? "" : "; deleting is off"
            );
    }
}
