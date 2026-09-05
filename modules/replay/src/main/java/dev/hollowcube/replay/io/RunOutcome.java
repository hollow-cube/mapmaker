package dev.hollowcube.replay.io;

/// Why a recording is being finished, which is not the same question as whether it is over.
///
/// Storage cannot tell these apart from the bytes, and nothing can recover the answer afterwards,
/// so the host says which at the moment it knows.
public enum RunOutcome {
    COMPLETED,
    /// Abandoned: a hard reset started a new run in its place.
    RESET
}
