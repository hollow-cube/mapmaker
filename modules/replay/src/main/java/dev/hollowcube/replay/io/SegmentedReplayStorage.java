package dev.hollowcube.replay.io;

import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

/// Storage for resumable segmented replay recordings.
///
/// Loading may block, so callers are responsible for doing it before handing the recording to the
/// tick thread. Writer methods are invoked from the recorder's virtual-thread write chain.
public interface SegmentedReplayStorage {

    /// Loads the latest committed recording, or null if this replay has not been recorded yet.
    @Blocking
    @Nullable SegmentedReplay load(String replayId);

    /// Returns a writer bound to the replay ID and the recording it will append to. Implementations
    /// use the base revision's token for optimistic concurrency. Creating the writer must not
    /// perform blocking I/O.
    SegmentedReplayWriter writer(String replayId, @Nullable SegmentedReplay base);
}
