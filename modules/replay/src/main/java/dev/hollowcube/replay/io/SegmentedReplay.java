package dev.hollowcube.replay.io;

import dev.hollowcube.replay.data.ReplayPreamble;
import org.jetbrains.annotations.Nullable;

/// A committed segmented recording, as read from storage.
///
/// The token opaquely identifies the exact revision this was read at. It is handed back to
/// [SegmentedReplayStorage#writer] so an implementation can make its commits conditional on nothing
/// else having appended in the meantime. Local storage has no use for it and leaves it null.
///
/// A finished recording must never be resumed. It is still returned so callers can tell the
/// difference between "nothing recorded yet" and "already complete", and it is the only case that
/// may omit a preamble, since storage that has already compacted one no longer has a segmented
/// preamble to hand back.
public record SegmentedReplay(
    @Nullable ReplayPreamble preamble,
    @Nullable String token,
    boolean finished
) {

    public SegmentedReplay {
        if (preamble == null && !finished)
            throw new IllegalArgumentException("an unfinished recording must have a preamble");
    }

    /// The preamble to resume from. Only valid on a recording that is not already finished.
    public ReplayPreamble requirePreamble() {
        if (preamble == null)
            throw new IllegalStateException("a finished recording cannot be resumed");
        return preamble;
    }
}
