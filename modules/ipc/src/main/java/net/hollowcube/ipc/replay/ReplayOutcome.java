package net.hollowcube.ipc.replay;

/// Why a recording will never be appended to again, as the recorder saw it.
///
/// The two are indistinguishable at the endpoint without reading the replay, which storage is not
/// allowed to do, so the recorder says which. [#FINISHED] is the ~4.6% of runs a player actually
/// completed; [#RESET] is the hard reset that superseded the save state, which is what the other
/// ~95% are, and the only reason to tell them apart is that a retention policy eventually will.
public enum ReplayOutcome {
    FINISHED,
    RESET,
    UNKNOWN
}
