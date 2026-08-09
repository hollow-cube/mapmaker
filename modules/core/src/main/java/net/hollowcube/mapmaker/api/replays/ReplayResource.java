package net.hollowcube.mapmaker.api.replays;

import org.jetbrains.annotations.Nullable;

/// The current preamble and storage state for a replay.
///
/// The ETag is opaque and must be passed back unchanged on the next conditional write.
public record ReplayResource(
    byte[] preamble,
    String etag,
    ReplayState state,
    ReplayRepresentation representation,
    @Nullable Integer nextSegmentIndex
) {

    public ReplayResource {
        if (preamble.length == 0)
            throw new IllegalArgumentException("preamble must not be empty");
        if (etag.isBlank())
            throw new IllegalArgumentException("ETag must not be blank");
        if (representation == ReplayRepresentation.COMPACTED && state != ReplayState.FINISHED)
            throw new IllegalArgumentException("a compacted replay must be finished");
        if (representation == ReplayRepresentation.SEGMENTED && nextSegmentIndex == null)
            throw new IllegalArgumentException("a segmented replay must include its next segment index");
        if (nextSegmentIndex != null && nextSegmentIndex < 0)
            throw new IllegalArgumentException("next segment index must not be negative");
    }
}
