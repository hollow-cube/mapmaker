package net.hollowcube.mapmaker.runtime.replay;

import dev.hollowcube.replay.io.SegmentedReplayCommit;
import dev.hollowcube.replay.io.SegmentedReplayWriter;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.api.replays.ReplayCommitRequest;
import net.hollowcube.mapmaker.api.replays.ReplayClient;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

/// Commits a segmented recording through the replay storage API.
///
/// Runs on the recorder's write chain, which is a virtual thread and is never the tick thread, so
/// blocking here is fine and retries may simply sleep.
final class ApiSegmentedReplayWriter implements SegmentedReplayWriter {
    private static final int MAX_ATTEMPTS = 3;
    private static final Duration RETRY_DELAY = Duration.ofMillis(250);

    private final ReplayClient client;
    private final String replayId;

    private @Nullable String etag;

    ApiSegmentedReplayWriter(ReplayClient client, String replayId, @Nullable String etag) {
        this.client = client;
        this.replayId = replayId;
        this.etag = etag;
    }

    @Override
    public void commit(SegmentedReplayCommit commit) {
        // Identical across attempts, which is what makes retrying safe: the backend returns the
        // original result rather than installing a second copy of the same segment.
        var request = new ReplayCommitRequest(
            etag,
            commit.idempotencyKey(),
            commit.segmentIndex(),
            commit.finished(),
            commit.preamble(),
            commit.segment()
        );

        for (var attempt = 1; ; attempt++) {
            try {
                this.etag = client.commit(replayId, request).etag();
                return;
            } catch (ApiClient.ConflictError | ApiClient.PreconditionFailedError e) {
                // Someone else advanced this recording. Commits cannot be merged, so this recorder
                // has to stop rather than reload and try to reconcile.
                throw new IllegalStateException("replay " + replayId + " was committed to concurrently", e);
            } catch (RuntimeException e) {
                if (attempt == MAX_ATTEMPTS) throw e;
                sleepBeforeRetry(e);
            }
        }
    }

    @Override
    public void close() {
        // Noop, the recording is committed incrementally.
    }

    private static void sleepBeforeRetry(RuntimeException failure) {
        try {
            Thread.sleep(RETRY_DELAY);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            failure.addSuppressed(e);
            throw failure;
        }
    }
}
