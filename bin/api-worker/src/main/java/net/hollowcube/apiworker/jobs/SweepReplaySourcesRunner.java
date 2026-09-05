package net.hollowcube.apiworker.jobs;

import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.apiworker.job.JobRunner;
import net.hollowcube.ipc.replay.ReplayService;
import net.hollowcube.ipc.util.IpcException;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/// Drops the segments a compacted replay was built from, and expires the idempotency records
/// nothing has ever swept.
///
/// Compaction on its own *increases* storage — the compacted object is added and every source
/// segment stays — so this is the pass that realises the win.
///
/// **Not bound in `Main` yet**: nothing has been compacted to sweep. Binding it is the switch and
/// [#deleting] is the second one, both guarding the same thing — a bad compaction found after its
/// sources are gone is not recoverable.
public final class SweepReplaySourcesRunner implements JobRunner<Void> {

    /// Long enough to cover a compaction that raced the sweep and any reader holding a segment,
    /// short enough that the win lands the same day.
    private static final Duration GRACE = Duration.ofHours(24);
    /// A record only has to outlive the retries of the request it recorded, which is minutes.
    private static final Duration IDEMPOTENCY_RETENTION = Duration.ofDays(7);

    private static final Logger logger = LoggerFactory.getLogger(SweepReplaySourcesRunner.class);

    private final ApiDatabase db;
    private final ReplayService replays;
    private final int limit;
    private final boolean deleting;

    public SweepReplaySourcesRunner(ApiDatabase db, ReplayService replays, int limit, boolean deleting) {
        this.db = db;
        this.replays = replays;
        this.limit = limit;
        this.deleting = deleting;
    }

    @Override
    public void run(@Nullable Void data) {
        var ids = db.replays.listCompactedReplaysWithSegments(Instant.now().minus(GRACE), limit);
        if (!deleting && !ids.isEmpty())
            logger.info("{} compacted replays have sources past the grace period; deleting is off", ids.size());

        var dropped = 0;
        for (var id : deleting ? ids : List.<String>of()) {
            try {
                // The api-server holds the credentials, and re-checks that the replay really is
                // compacted, which is what makes a stale id here harmless.
                dropped += replays.dropSegments(id);
            } catch (IpcException e) {
                // Gone, or no longer compacted. Not worth failing the pass; the next one sees it.
                logger.warn("could not drop the sources of replay {}: {}", id, e.getMessage());
            }
        }
        if (dropped > 0) logger.info("dropped {} source segments across {} replays", dropped, ids.size());

        var expired = db.replays.deleteExpiredReplayIdempotency(Instant.now().minus(IDEMPOTENCY_RETENTION));
        if (expired > 0) logger.info("expired {} replay idempotency records", expired);
    }
}
