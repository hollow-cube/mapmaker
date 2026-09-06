package net.hollowcube.ipc.map;

public enum DeleteVerificationResult {
    /// Verification and its runs were reset, whether or not there was anything to reset.
    RESET,
    MAP_NOT_FOUND,
    MAP_PUBLISHED,
    UNKNOWN,
}
