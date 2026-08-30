package net.hollowcube.ipc.chat;

/// Where a player is speaking.
///
/// Replaces the Go string that was `global`/`local`/`staff`/`reply` or, for a direct message, the
/// target's uuid — which meant every reader of it started by asking whether it parsed as a uuid.
/// The target is its own parameter now, and [#REPLY] is resolved to one by the api.
public enum ChatChannel {
    GLOBAL,
    /// Everyone in the same map as the sender. The sender's map rides along on the message so a
    /// recipient can answer that from its own world rather than from a session presence lookup.
    LOCAL,
    STAFF,
    /// A direct message, to `targetId`.
    DIRECT,
    /// A direct message to whoever the sender last exchanged one with.
    REPLY,
    UNKNOWN
}
