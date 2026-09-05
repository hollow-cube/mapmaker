package net.hollowcube.mapmaker.runtime.replay.playback;

import dev.hollowcube.replay.ReplayCompactor;
import dev.hollowcube.replay.io.SegmentedFileReplaySource;
import dev.hollowcube.replay.io.SegmentedFileReplayStorage;
import net.hollowcube.ipc.replay.ReplayService;
import net.hollowcube.ipc.util.IpcException;
import net.hollowcube.mapmaker.runtime.parkour.replay.ReplayManager;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

/// Where a compacted replay comes from.
///
/// Only compacted replays are readable, so this is also the seam where a recording that is still
/// segmented gets compacted, whether that happens here or in storage.
@FunctionalInterface
public interface CompactedReplaySource {

    /// Loads a replay whole, or null if nothing was recorded under this ID.
    @Blocking
    byte @Nullable [] load(String replayId);

    /// Replay storage, which serves the compacted form directly.
    static CompactedReplaySource api(ReplayService replays) {
        return replayId -> {
            try (var blob = replays.getCompacted(replayId, null, null)) {
                return blob.readAllBytes();
            } catch (IpcException e) {
                // 409 is a replay nothing has compacted yet: from here, nothing to play either.
                if (e.status() == 404 || e.status() == 409) return null;
                throw e;
            } catch (IOException e) {
                throw new UncheckedIOException("reading compacted replay " + replayId + " failed", e);
            }
        };
    }

    /// A local recording directory, compacted on every load since nothing persists the result.
    static CompactedReplaySource local(Path root) {
        return replayId -> {
            var recording = new SegmentedFileReplayStorage(root).load(replayId);
            if (recording == null) return null;
            return ReplayCompactor.compact(
                recording.requirePreamble(),
                new SegmentedFileReplaySource(root.resolve(replayId)),
                null, ReplayManager.REGISTRY
            ).data();
        };
    }
}
