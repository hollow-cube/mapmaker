package net.hollowcube.ipc.player;

import org.jetbrains.annotations.Nullable;

public sealed interface BlockResult {

    record Blocked() implements BlockResult {}

    record AlreadyBlocked() implements BlockResult {}

    record TargetIsStaff() implements BlockResult {}

    record Unknown(@Nullable String type) implements BlockResult {}
}
