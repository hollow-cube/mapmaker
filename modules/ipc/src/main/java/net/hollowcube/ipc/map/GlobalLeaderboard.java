package net.hollowcube.ipc.map;

/// The hub boards: maps beaten counts every published map with a completed run; top times counts
/// the published ascending time boards where the player holds the fastest time.
public enum GlobalLeaderboard {
    TOP_TIMES,
    MAPS_BEATEN,
    UNKNOWN,
}
