package net.hollowcube.mapmaker.runtime.replay.playback;

import dev.hollowcube.replay.ReplayCompactor;
import dev.hollowcube.replay.io.SegmentedFileReplaySource;
import dev.hollowcube.replay.io.SegmentedFileReplayStorage;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.api.replays.ReplayClient;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

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
    static CompactedReplaySource api(ReplayClient client) {
        return replayId -> {
            try {
                return client.getCompacted(replayId).data();
            } catch (ApiClient.NotFoundError _) {
                return null;
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
                new SegmentedFileReplaySource(root.resolve(replayId))
            ).data();
        };
    }
}
