package net.hollowcube.ipc.map;

import org.jetbrains.annotations.Nullable;

public sealed interface OpenWorldResult {

    /// @param headSeq the last write-ahead-log record stored or saved for the map, 0 if none
    record Opened(long headSeq) implements OpenWorldResult {}

    record OpenElsewhere(String serverId) implements OpenWorldResult {}

    record Unknown(@Nullable String type) implements OpenWorldResult {}
}
