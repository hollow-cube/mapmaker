package net.hollowcube.mapmaker.api.replays;

import org.jetbrains.annotations.Nullable;

/// Storage state after a successful recording commit or compacted publication.
public record ReplayWriteResult(
    String etag,
    ReplayState state,
    ReplayRepresentation representation,
    @Nullable Integer nextSegmentIndex
) {

    public ReplayWriteResult {
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
