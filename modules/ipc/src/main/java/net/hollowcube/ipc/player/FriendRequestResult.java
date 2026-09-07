package net.hollowcube.ipc.player;

import org.jetbrains.annotations.Nullable;

public sealed interface FriendRequestResult {
    record Sent() implements FriendRequestResult {}

    record Accepted() implements FriendRequestResult {}

    record AlreadyFriends() implements FriendRequestResult {}

    record AlreadyRequested() implements FriendRequestResult {}

    record LimitReached(
        int limit,
        int friendCount,
        int outgoingRequestCount
    ) implements FriendRequestResult {}

    record TargetAutoRejects() implements FriendRequestResult {}

    record BlockedTarget() implements FriendRequestResult {}

    record BlockedByTarget() implements FriendRequestResult {}

    record Unknown(@Nullable String type) implements FriendRequestResult {}
}
