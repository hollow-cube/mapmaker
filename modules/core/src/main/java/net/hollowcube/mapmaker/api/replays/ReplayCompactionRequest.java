package net.hollowcube.mapmaker.api.replays;

import java.util.UUID;

/// A complete compacted replay produced from a finished segmented recording.
public record ReplayCompactionRequest(
    String expectedEtag,
    UUID idempotencyKey,
    int preambleLength,
    byte[] recording
) {

    public ReplayCompactionRequest {
        if (expectedEtag.isBlank())
            throw new IllegalArgumentException("expected ETag must not be blank");
        if (preambleLength <= 0 || preambleLength > recording.length)
            throw new IllegalArgumentException("preamble length must fit within the recording");
    }
}
