package net.hollowcube.ipc.replay;

/// How a replay is stored: as the segments it was recorded in, or as the one object those were
/// merged into.
public enum ReplayRepresentation {
    SEGMENTED,
    /// Playable. A segmented recording's chunk offsets are relative to a segment, so nothing can
    /// read one until it has been compacted.
    COMPACTED,
    UNKNOWN
}
