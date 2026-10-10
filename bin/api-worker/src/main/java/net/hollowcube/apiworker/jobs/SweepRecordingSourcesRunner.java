package net.hollowcube.apiworker.jobs;

import dev.hollowcube.replay.data.ReplayHeader;
import dev.hollowcube.replay.data.ReplayPreamble;
import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.apiworker.job.JobRunner;
import net.hollowcube.ipc.replay.ReplayService;
import net.hollowcube.ipc.util.IpcException;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

/// Drops the segments below the first one a recording's preamble references.
///
/// [TranscodeReplayRunner] rewrites an idle recording into one new segment and leaves the old ones
/// as sources, which [SweepReplaySourcesRunner] never reaches because the replay is not compacted,
/// and may never be if nobody resumes it. Storage cannot read a preamble, so the cut is found here
/// and handed over with the revision it was read at.
public final class SweepRecordingSourcesRunner implements JobRunner<Void> {

    /// The same as [SweepReplaySourcesRunner]'s, for the same reason.
    private static final Duration GRACE = Duration.ofHours(24);
    private static final int PAGE = 1000;

    private static final Logger logger = LoggerFactory.getLogger(SweepRecordingSourcesRunner.class);

    private final ApiDatabase db;
    private final ReplayService replays;
    private final boolean deleting;

    public SweepRecordingSourcesRunner(ApiDatabase db, ReplayService replays, boolean deleting) {
        this.db = db;
        this.replays = replays;
        this.deleting = deleting;
    }

    @Override
    public void run(@Nullable Void data) {
        var before = Instant.now().minus(GRACE);
        var after = "";
        var found = 0;
        var dropped = 0;
        while (true) {
            var page = db.replays.listIdleRecordings(before, after, PAGE);
            if (page.isEmpty()) break;
            after = page.getLast().id();

            for (var recording : page) {
                if (recording.firstSegment() == null) continue;
                var cut = firstReferenced(recording.currentPreamble());
                if (cut <= recording.firstSegment()) continue;

                found++;
                if (!deleting) continue;
                try {
                    dropped += replays.dropSegmentsBefore(recording.id(), recording.version(), cut);
                } catch (IpcException e) {
                    // Resumed or finished since the page was read; the next pass sees it as it is.
                    logger.warn(
                        "could not drop the old segments of recording {}: {}",
                        recording.id(),
                        e.getMessage()
                    );
                }
            }
        }
        if (found > 0)
            logger.info(
                "{} recordings have segments their preamble no longer references{}",
                found,
                deleting ? "; dropped " + dropped : "; deleting is off"
            );
    }

    /// The lowest segment any chunk is in, or -1 for a preamble with nothing to cut by.
    private static int firstReferenced(byte[] preamble) {
        if (!ReplayHeader.readable(ReplayHeader.versionOf(preamble))) return -1;
        var first = -1;
        for (var chunk : ReplayPreamble.index(preamble)) {
            var segment = ReplayPreamble.segmentIndex(chunk);
            if (first < 0 || segment < first) first = segment;
        }
        return first;
    }
}
