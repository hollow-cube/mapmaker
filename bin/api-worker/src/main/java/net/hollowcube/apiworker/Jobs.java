package net.hollowcube.apiworker;

import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.apiserver.job.JobSpec;
import net.hollowcube.apiworker.job.Worker;
import net.hollowcube.apiworker.jobs.*;
import net.hollowcube.ipc.replay.ReplayClient;
import net.hollowcube.mapmaker.api.maps.MapClient;
import net.hollowcube.posthog.PostHog;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

public final class Jobs {

    public record ReplayConfig(
        ReplayClient client,
        int reconcileBatch,
        int sweepBatch,
        boolean sweepSources,
        boolean sweepObjects,
        Duration recordingIdle
    ) {}

    public static void register(
        Worker worker,
        ApiDatabase db,
        MapClient maps,
        @Nullable Jobs.ReplayConfig replays
    ) {
        worker.handle(JobSpec.PLAYER_COUNT, new PlayerCountRunner(db, PostHog.getClient()));
        worker.handle(JobSpec.MAP_STATS, new MapStatsRunner(db, false));
        worker.handle(JobSpec.MAP_STATS_FULL, new MapStatsRunner(db, true));
        worker.handle(JobSpec.INDEX_MAP, new IndexMapRunner(db, maps));
        if (replays == null) return;

        var client = replays.client();
        worker.handle(JobSpec.COMPACT_REPLAY, new CompactReplayRunner(client));
        worker.handle(
            JobSpec.RECONCILE_REPLAYS,
            new ReconcileReplaysRunner(db, replays.reconcileBatch())
        );
        worker.handle(
            JobSpec.SWEEP_REPLAY_SOURCES,
            new SweepReplaySourcesRunner(db, client, replays.sweepBatch(), replays.sweepSources())
        );
        worker.handle(
            JobSpec.SWEEP_RECORDING_SOURCES,
            new SweepRecordingSourcesRunner(db, client, replays.sweepSources())
        );
        worker.handle(
            JobSpec.SWEEP_REPLAY_OBJECTS,
            new SweepReplayObjectsRunner(client, replays.sweepObjects())
        );
        worker.handle(
            JobSpec.TRANSCODE_REPLAY,
            new TranscodeReplayRunner(client, replays.recordingIdle())
        );
    }

    private Jobs() {}
}
