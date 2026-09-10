package net.hollowcube.ipc.map;

import java.util.UUID;

/// Where a player is on a map they have started: `playtime` is the best completed run in
/// milliseconds when complete, otherwise the latest run so far. A map never started has no entry.
public record PlayerMapProgress(UUID mapId, Progress progress, long playtime) {

    public enum Progress {
        STARTED,
        COMPLETE,
        UNKNOWN,
    }
}
