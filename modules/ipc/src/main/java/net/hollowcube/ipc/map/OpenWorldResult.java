package net.hollowcube.ipc.map;

import org.jetbrains.annotations.Nullable;

public sealed interface OpenWorldResult {

    record Opened() implements OpenWorldResult {}

    record OpenElsewhere(String serverId) implements OpenWorldResult {}

    record Unknown(@Nullable String type) implements OpenWorldResult {}
}
