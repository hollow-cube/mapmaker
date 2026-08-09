package net.hollowcube.mapmaker.api.replays;

/// A complete or ranged replay-stream response.
public record ReplayStream(
    byte[] data,
    String etag,
    long offset,
    long totalLength
) {

    public ReplayStream {
        if (offset < 0)
            throw new IllegalArgumentException("stream offset must not be negative");
        if (totalLength < 0 || offset > totalLength)
            throw new IllegalArgumentException("invalid stream total length");
        if (data.length > totalLength - offset)
            throw new IllegalArgumentException("stream data exceeds its declared total length");
    }
}
