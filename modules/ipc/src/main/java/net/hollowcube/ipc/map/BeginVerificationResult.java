package net.hollowcube.ipc.map;

public enum BeginVerificationResult {
    /// The editor drained and verification is pending, or already was on a retry.
    READY,
    MAP_NOT_FOUND,
    MAP_PUBLISHED,
    /// Building maps are not verified.
    NOT_VERIFIABLE,
    /// The editing world was still registered when the drain timeout ran out; nothing changed,
    /// and the call can be repeated.
    DRAIN_TIMEOUT,
    UNKNOWN,
}
