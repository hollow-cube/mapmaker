package net.hollowcube.ipc.map;

import org.jetbrains.annotations.Nullable;

public sealed interface OpenWorldResult {

    /// @param headSeq the last write-ahead-log record stored or saved for the map, 0 if none
    /// @param savedSeq the last record already in the world [MapService#getWorld] answers; the records
    ///     after it, through `headSeq`, are in [MapService#getWal] and not in that world
    record Opened(long headSeq, long savedSeq) implements OpenWorldResult {}

    record OpenElsewhere(String serverId) implements OpenWorldResult {}

    record Unknown(@Nullable String type) implements OpenWorldResult {}
}
