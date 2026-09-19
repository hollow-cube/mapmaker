package net.hollowcube.apiworker.jobs;

import dev.hollowcube.replay.ReplayCompactor;
import dev.hollowcube.replay.data.ReplayPreamble;
import net.hollowcube.apiserver.common.Digest;
import net.hollowcube.apiserver.job.TranscodeReplay;
import net.hollowcube.apiworker.job.JobRunner;
import net.hollowcube.ipc.Blob;
import net.hollowcube.ipc.replay.ReplayCommit;
import net.hollowcube.ipc.replay.ReplayCompaction;
import net.hollowcube.ipc.replay.ReplayService;
import net.hollowcube.ipc.replay.ReplayState;
import net.hollowcube.ipc.util.IpcException;
import net.hollowcube.mapmaker.runtime.parkour.replay.ReplayManager;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;

/// Rewrites a replay whose chunks are behind this build's format or data version: a finished one
/// by publishing a compacted replay, a recording by committing one new segment holding every chunk.
/// Reading upgrades every event, so writing it back is what makes the stored bytes current.
///
/// Recordings are only rewritten once idle for `recordingIdle`, since they can still be resumed. A
/// session that resumes the old preamble anyway fails its next commit on the revision.
public final class TranscodeReplayRunner implements JobRunner<TranscodeReplay> {

    private static final Logger logger = LoggerFactory.getLogger(TranscodeReplayRunner.class);

    private final ReplayService replays;
    private final Duration recordingIdle;

    public TranscodeReplayRunner(ReplayService replays, Duration recordingIdle) {
        this.replays = replays;
        this.recordingIdle = recordingIdle;
    }

    @Override
    public void run(@Nullable TranscodeReplay data) throws IOException {
        if (data == null)
            throw new IllegalArgumentException(
                "transcode-replay needs data: {\"replayId\": \"...\"}"
            );
        var id = data.replayId();

        var info = replays.getReplay(id);
        if (info == null) {
            logger.info("replay {} is gone ({}), dropping", id, data.reason());
            return;
        }
        var finished = info.state() == ReplayState.FINISHED;
        if (!finished && System.currentTimeMillis() - info.updatedAt() < recordingIdle.toMillis())
            return;

        final byte[] preamble;
        try (var blob = replays.getPreamble(id, info.revision())) {
            preamble = blob.readAllBytes();
        } catch (IpcException e) {
            if (e.status() == 412) return;
            throw e;
        }
        if (!StoredReplay.needsRewrite(preamble)) return;

        var start = System.nanoTime();
        var stored = StoredReplay.read(replays, info, preamble);
        if (!finished) {
            rewriteRecording(stored, data.reason(), start);
            return;
        }
        var transcoded = stored.transcode();

        try {
            replays.publishCompacted(
                new ReplayCompaction(
                    id,
                    info.revision(),
                    CompactReplayRunner.idempotencyKey("transcode", id, info.revision()),
                    transcoded.preambleLength(),
                    Digest.base64(Digest.sha256(transcoded.data()))
                ),
                Blob.of(transcoded.data())
            );
        } catch (IpcException e) {
            // Compaction landed first, which transcodes too.
            if (e.status() == 412) return;
            throw e;
        }

        logger.info(
            "transcoded {} ({}, {}) from {} to {} bytes in {}ms",
            id,
            info.representation(),
            data.reason(),
            stored.storedBytes(),
            transcoded.data().length,
            (System.nanoTime() - start) / 1_000_000
        );
    }

    private void rewriteRecording(StoredReplay stored, String reason, long start) {
        var info = stored.info();
        var segmentIndex = Objects.requireNonNull(
            info.nextSegmentIndex(),
            "a recording has a next segment"
        );
        var rewrite = ReplayCompactor.transcodeRecording(
            ReplayPreamble.read(stored.preamble()),
            stored.segments()::get,
            segmentIndex,
            ReplayManager.REGISTRY
        );
        var body = new byte[rewrite.preamble().length + rewrite.segment().length];
        System.arraycopy(rewrite.preamble(), 0, body, 0, rewrite.preamble().length);
        System.arraycopy(
            rewrite.segment(),
            0,
            body,
            rewrite.preamble().length,
            rewrite.segment().length
        );

        try {
            replays.commit(
                new ReplayCommit(
                    info.id(),
                    info.revision(),
                    CompactReplayRunner.idempotencyKey("transcode", info.id(), info.revision()),
                    rewrite.preamble().length,
                    segmentIndex,
                    false,
                    null,
                    Digest.base64(Digest.sha256(body))
                ),
                Blob.of(body)
            );
        } catch (IpcException e) {
            if (e.status() == 412 || e.status() == 409) return;
            throw e;
        }

        logger.info(
            "transcoded recording {} ({}) from {} to {} bytes into segment {} in {}ms",
            info.id(),
            reason,
            stored.storedBytes(),
            body.length,
            segmentIndex,
            (System.nanoTime() - start) / 1_000_000
        );
    }
}
