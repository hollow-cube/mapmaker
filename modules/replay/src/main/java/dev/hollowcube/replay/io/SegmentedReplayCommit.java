package dev.hollowcube.replay.io;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/// A single atomic recording commit.
///
/// The preamble always describes the complete recording including this commit. A commit carries at
/// most one new segment, and a commit without one is only valid when it finalizes the recording.
///
/// The idempotency key is stable for the life of the commit, so a writer may retry the exact same
/// bytes after an ambiguous failure without risking a duplicate segment.
///
/// @param outcome why the recording is being finished, on the commit that finishes it and nowhere
///                else. Storage cannot work it out for itself and cannot recover it later.
public record SegmentedReplayCommit(
    UUID idempotencyKey,
    byte[] preamble,
    @Nullable Integer segmentIndex,
    byte[] segment,
    boolean finished,
    @Nullable RunOutcome outcome
) {

    public SegmentedReplayCommit {
        if (preamble.length == 0)
            throw new IllegalArgumentException("preamble must not be empty");
        if (segmentIndex != null && segmentIndex < 0)
            throw new IllegalArgumentException("segment index must not be negative");
        if (segmentIndex == null && segment.length != 0)
            throw new IllegalArgumentException("segment index is required when segment bytes are present");
        if (segmentIndex != null && segment.length == 0)
            throw new IllegalArgumentException("segment bytes are required when a segment index is present");
        if (segmentIndex == null && !finished)
            throw new IllegalArgumentException("a commit without a segment must finalize the recording");
        if (finished == (outcome == null))
            throw new IllegalArgumentException("an outcome belongs to the commit that finalizes the recording, and only to it");
    }

    public boolean hasSegment() {
        return segmentIndex != null;
    }
}
