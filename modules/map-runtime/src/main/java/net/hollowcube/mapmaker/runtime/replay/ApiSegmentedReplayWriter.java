package net.hollowcube.mapmaker.runtime.replay;

import dev.hollowcube.replay.io.RunOutcome;
import dev.hollowcube.replay.io.SegmentedReplayCommit;
import dev.hollowcube.replay.io.SegmentedReplayWriter;
import net.hollowcube.ipc.Blob;
import net.hollowcube.ipc.replay.ReplayCommit;
import net.hollowcube.ipc.replay.ReplayOutcome;
import net.hollowcube.ipc.replay.ReplayService;
import net.hollowcube.ipc.util.IpcException;
import org.jetbrains.annotations.Nullable;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;

/// Commits a segmented recording through the replay ipc service.
///
/// Runs on the recorder's write chain, which is a virtual thread and is never the tick thread, so
/// blocking here is fine and retries may simply sleep.
final class ApiSegmentedReplayWriter implements SegmentedReplayWriter {
    private static final int MAX_ATTEMPTS = 3;
    private static final Duration RETRY_DELAY = Duration.ofMillis(250);

    private final ReplayService replays;
    private final String replayId;

    private @Nullable Long revision;

    ApiSegmentedReplayWriter(ReplayService replays, String replayId, @Nullable Long revision) {
        this.replays = replays;
        this.replayId = replayId;
        this.revision = revision;
    }

    @Override
    public void commit(SegmentedReplayCommit commit) {
        var body = new byte[commit.preamble().length + commit.segment().length];
        System.arraycopy(commit.preamble(), 0, body, 0, commit.preamble().length);
        System.arraycopy(commit.segment(), 0, body, commit.preamble().length, commit.segment().length);

        // Identical across attempts, which is what makes retrying safe: the backend returns the
        // original result rather than installing a second copy of the same segment.
        var meta = new ReplayCommit(replayId, revision, commit.idempotencyKey().toString(),
            commit.preamble().length, commit.segmentIndex(), commit.finished(),
            outcome(commit.outcome()), digest(body));

        for (var attempt = 1; ; attempt++) {
            try {
                revision = replays.commit(meta, Blob.of(body)).revision();
                return;
            } catch (IpcException e) {
                // Someone else advanced this recording. Commits cannot be merged, so this recorder
                // has to stop rather than reload and try to reconcile.
                if (e.status() == 409 || e.status() == 412)
                    throw new IllegalStateException("replay " + replayId + " was committed to concurrently", e);
                if (attempt == MAX_ATTEMPTS) throw e;
                sleepBeforeRetry(e);
            }
        }
    }

    @Override
    public void close() {
        // Noop, the recording is committed incrementally.
    }

    /// Two enums because the replay format knows nothing about this api — and `FINISHED` there
    /// would mean the recording is closed, not that the run was played out.
    private static @Nullable ReplayOutcome outcome(@Nullable RunOutcome outcome) {
        if (outcome == null) return null;
        return switch (outcome) {
            case COMPLETED -> ReplayOutcome.FINISHED;
            case RESET -> ReplayOutcome.RESET;
        };
    }

    private static String digest(byte[] body) {
        try {
            return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(body));
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("SHA-256 is unavailable", e);
        }
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
