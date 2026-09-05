package net.hollowcube.apiworker.jobs;

import dev.hollowcube.replay.ReplayCompactor;
import dev.hollowcube.replay.data.ReplayHeader;
import dev.hollowcube.replay.data.ReplayPreamble;
import net.hollowcube.apiserver.common.Digest;
import net.hollowcube.apiserver.job.CompactReplay;
import net.hollowcube.apiworker.job.JobRunner;
import net.hollowcube.ipc.Blob;
import net.hollowcube.ipc.replay.ReplayCompaction;
import net.hollowcube.ipc.replay.ReplayRepresentation;
import net.hollowcube.ipc.replay.ReplayService;
import net.hollowcube.ipc.replay.ReplayState;
import net.hollowcube.ipc.util.IpcException;
import net.hollowcube.mapmaker.runtime.parkour.replay.ReplayManager;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;

/// Merges a finished replay's segments into the one object that makes it playable.
///
/// Nothing had ever done this — the `PUT` endpoint shipped with the feature and was never called —
/// so every replay ever recorded is still segmented and unplayable. Compaction is a pure function
/// of the committed segments, so repeating a run costs CPU and nothing else.
public final class CompactReplayRunner implements JobRunner<CompactReplay> {

    /// What `replay_idempotency.idempotency_key` accepts.
    private static final int MAX_KEY_LENGTH = 512;

    private static final Logger logger = LoggerFactory.getLogger(CompactReplayRunner.class);

    private final ReplayService replays;

    public CompactReplayRunner(ReplayService replays) {
        this.replays = replays;
    }

    @Override
    public void run(@Nullable CompactReplay data) throws IOException {
        if (data == null) throw new IllegalArgumentException("compact-replay needs data: {\"replayId\": \"...\"}");
        var id = data.replayId();

        var info = replays.getReplay(id);
        if (info == null) {
            logger.info("replay {} is gone ({}), dropping", id, data.reason());
            return;
        }
        // Both ordinary: the commit and the reconciler each enqueue, so a row may arrive while the
        // recording is still open or after another replica has done the work.
        if (info.state() != ReplayState.FINISHED) return;
        if (info.representation() != ReplayRepresentation.SEGMENTED) return;

        final byte[] preamble;
        try (var blob = replays.getPreamble(id, info.revision())) {
            preamble = blob.readAllBytes();
        }

        // Nothing can read an older format, and the reconciler keeps offering these because they
        // stay finished and segmented — so returning rather than throwing costs one attempt a tick
        // instead of five and a parked row. They compact when something can convert them.
        var version = ReplayHeader.versionOf(preamble);
        if (version != ReplayHeader.VERSION_LATEST) {
            logger.info("replay {} is format version {}, which this build cannot read; leaving it segmented",
                id, version);
            return;
        }

        var start = System.nanoTime();
        var compacted = ReplayCompactor.compact(ReplayPreamble.read(preamble), index -> segment(id, index),
            null, ReplayManager.REGISTRY);

        try {
            replays.publishCompacted(new ReplayCompaction(id, info.revision(),
                    compactionKey(id, info.revision()), compacted.preambleLength(),
                    Digest.base64(Digest.sha256(compacted.data()))),
                Blob.of(compacted.data()));
        } catch (IpcException e) {
            // A commit landed while this was compacting, or another replica got there first. The
            // newer commit enqueued a fresh row of its own.
            if (e.status() == 412) return;
            throw e;
        }

        logger.info("compacted {} ({}) to {} bytes in {}ms", id, data.reason(),
            compacted.data().length, (System.nanoTime() - start) / 1_000_000);
    }

    /// Derived from the replay and the revision it was compacted from, never random: a random key
    /// means a retry after a lost response publishes a second object and orphans the first, which is
    /// what `ApiReplayCompactor` did.
    static String compactionKey(String replayId, long sourceRevision) {
        var key = "compact:" + replayId + ":" + sourceRevision;
        // A replay id may be 512 characters on its own, which is the whole column. Hashing keeps it
        // deterministic, which is all the key has to be.
        return key.length() <= MAX_KEY_LENGTH
            ? key
            : "compact:" + Digest.hex(Digest.sha256(replayId + ":" + sourceRevision));
    }

    private byte[] segment(String id, int index) {
        try (var blob = replays.getSegment(id, index)) {
            return blob.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("reading segment " + index + " of replay " + id + " failed", e);
        }
    }

}
