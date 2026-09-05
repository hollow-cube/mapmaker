package net.hollowcube.ipc.replay;

/// Whether anything may still be appended to a recording.
public enum ReplayState {
    RECORDING,
    /// One way: no commit after this one is accepted, whether or not the run was completed.
    FINISHED,
    UNKNOWN
}
