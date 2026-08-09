package dev.hollowcube.replay.io;

public interface SegmentedReplayWriter {

    /// Atomically installs this commit. Its segment, if any, must be durable before the new
    /// preamble becomes visible, and neither may become visible without the other.
    ///
    /// Implementations may retry internally using the commit's idempotency key. Throwing marks the
    /// recording as failed; the recorder stops and does not attempt to reconcile.
    void commit(SegmentedReplayCommit commit);

    /// Releases writer resources. This does not make the replay permanently immutable; unless a
    /// commit finalized it, a later recording session may load its preamble and append new segments.
    void close();

}
