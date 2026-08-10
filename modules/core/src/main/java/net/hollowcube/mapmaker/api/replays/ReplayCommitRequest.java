package net.hollowcube.mapmaker.api.replays;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/// A single atomic recording commit.
///
/// A null expected ETag creates the replay with `If-None-Match: *`. A null segment index is only
/// valid for a metadata-only final commit.
public record ReplayCommitRequest(
    @Nullable String expectedEtag,
    UUID idempotencyKey,
    @Nullable Integer segmentIndex,
    boolean finished,
    byte[] preamble,
    byte[] segment
) {

    public ReplayCommitRequest {
        if (expectedEtag != null && expectedEtag.isBlank())
            throw new IllegalArgumentException("expected ETag must not be blank");
        if (segmentIndex != null && segmentIndex < 0)
            throw new IllegalArgumentException("segment index must not be negative");
        if (preamble.length == 0)
            throw new IllegalArgumentException("preamble must not be empty");
        if (segmentIndex == null && segment.length != 0)
            throw new IllegalArgumentException("segment index is required when segment bytes are present");
        if (segmentIndex != null && segment.length == 0)
            throw new IllegalArgumentException("segment bytes are required when a segment index is present");
        if (segmentIndex == null && !finished)
            throw new IllegalArgumentException("a commit without a segment must finalize the replay");
    }
}
