package net.hollowcube.mapmaker.runtime.replay;

import dev.hollowcube.replay.data.ReplayPreamble;
import dev.hollowcube.replay.io.SegmentedReplay;
import dev.hollowcube.replay.io.SegmentedReplayStorage;
import dev.hollowcube.replay.io.SegmentedReplayWriter;
import net.hollowcube.ipc.replay.ReplayRepresentation;
import net.hollowcube.ipc.replay.ReplayService;
import net.hollowcube.ipc.replay.ReplayState;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;

/// Segmented replay storage backed by the replay ipc service.
public final class ApiSegmentedReplayStorage implements SegmentedReplayStorage {
    private final ReplayService replays;

    public ApiSegmentedReplayStorage(ReplayService replays) {
        this.replays = replays;
    }

    @Override
    public @Nullable SegmentedReplay load(String replayId) {
        var info = replays.getReplay(replayId);
        if (info == null) return null; // Nothing recorded yet; the first commit creates it.

        // A compacted preamble carries absolute file offsets rather than packed segment offsets, so
        // it cannot be read as a segmented recording. Nothing can append to it either, so all the
        // recorder needs to know is that it is finished.
        if (info.representation() == ReplayRepresentation.COMPACTED)
            return new SegmentedReplay(null, token(info.revision()), true);

        // Conditional on the revision the state came from, so the recorder resumes out of one
        // revision rather than two reads of a replay that moved in between.
        final byte[] preambleBytes;
        try (var blob = replays.getPreamble(replayId, info.revision())) {
            preambleBytes = blob.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("reading the preamble of replay " + replayId + " failed", e);
        }

        var preamble = ReplayPreamble.read(preambleBytes);
        var nextSegmentIndex = info.nextSegmentIndex();
        if (nextSegmentIndex != null && nextSegmentIndex != preamble.nextSegmentIndex()) {
            throw new IllegalStateException(
                "replay " + replayId + " expects segment " + nextSegmentIndex
                    + " but its preamble ends at " + preamble.nextSegmentIndex()
            );
        }

        return new SegmentedReplay(preamble, token(info.revision()), info.state() == ReplayState.FINISHED);
    }

    @Override
    public SegmentedReplayWriter writer(String replayId, @Nullable SegmentedReplay base) {
        return new ApiSegmentedReplayWriter(replays, replayId,
            base == null || base.token() == null ? null : Long.parseLong(base.token()));
    }

    /// [SegmentedReplay#token] is opaque to the replay format; storage's is the revision.
    private static String token(long revision) {
        return Long.toString(revision);
    }
}
