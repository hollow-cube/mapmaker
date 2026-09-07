package net.hollowcube.ipc.player;

import net.hollowcube.ipc.PaginatedList;
import net.hollowcube.ipc.util.Ipc;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/// Pages are zero-based.
@Ipc
public interface SocialService {

    PaginatedList<Friend> friends(UUID playerId, boolean onlineOnly, int page, int pageSize);

    List<UUID> onlineFriendIds(UUID playerId);

    boolean removeFriend(UUID playerId, UUID targetId);

    PaginatedList<FriendRequest> incomingFriendRequests(UUID playerId, int page, int pageSize);

    PaginatedList<FriendRequest> outgoingFriendRequests(UUID playerId, int page, int pageSize);

    /// Accepts an existing request in the opposite direction.
    FriendRequestResult sendFriendRequest(UUID playerId, UUID targetId);

    @Nullable
    FriendRequest deleteFriendRequest(UUID playerId, UUID targetId, boolean bidirectional);

    BlockResult block(UUID playerId, UUID targetId);

    boolean unblock(UUID playerId, UUID targetId);

    PaginatedList<Block> blocks(UUID playerId, int page, int pageSize);

    List<Block> blocksBetween(UUID playerId, UUID targetId, boolean bidirectional);
}
