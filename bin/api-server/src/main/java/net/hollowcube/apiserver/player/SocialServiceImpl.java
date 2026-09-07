package net.hollowcube.apiserver.player;

import com.google.gson.JsonPrimitive;
import net.hollowcube.apiserver.common.Json;
import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.apiserver.db.PlayerData;
import net.hollowcube.apiserver.db.PlayerFriendRequests;
import net.hollowcube.apiserver.db.SocialQueries;
import net.hollowcube.apiserver.notification.NotificationServiceImpl;
import net.hollowcube.ipc.PaginatedList;
import net.hollowcube.ipc.player.Block;
import net.hollowcube.ipc.player.BlockResult;
import net.hollowcube.ipc.player.Friend;
import net.hollowcube.ipc.player.FriendRequest;
import net.hollowcube.ipc.player.FriendRequestResult;
import net.hollowcube.ipc.player.SocialService;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static net.hollowcube.ipc.PaginatedList.limit;
import static net.hollowcube.ipc.PaginatedList.offset;
import static net.hollowcube.ipc.util.IpcException.badRequest;
import static net.hollowcube.ipc.util.IpcException.notFound;

public final class SocialServiceImpl implements SocialService {

    private static final String FRIEND_REQUEST = "friend_request";
    private static final String FRIEND_ADDED = "friend_added";
    private static final String AUTO_REJECT_SETTING = "auto_reject_friend_requests";

    private static final int FREE_FRIEND_LIMIT = 30;
    private static final int EXTENDED_FRIEND_LIMIT = 150;

    private final ApiDatabase db;
    private final NotificationServiceImpl notifications;

    public SocialServiceImpl(ApiDatabase db, NotificationServiceImpl notifications) {
        this.db = db;
        this.notifications = notifications;
    }

    @Override
    public PaginatedList<Friend> friends(
        UUID playerId,
        boolean onlineOnly,
        int page,
        int pageSize
    ) {
        var rows = db.social.getFriends(
            playerId,
            onlineOnly,
            offset(page, pageSize),
            limit(pageSize)
        );
        return PaginatedList.of(
            rows,
            SocialQueries.GetFriendsRow::totalCount,
            row -> new Friend(
                Players.stub(row.player()),
                row.online(),
                row.player().lastOnline(),
                row.friendsSince()
            )
        );
    }

    @Override
    public List<UUID> onlineFriendIds(UUID playerId) {
        return db.social.getOnlineFriendIds(playerId);
    }

    @Override
    public boolean removeFriend(UUID playerId, UUID targetId) {
        if (playerId.equals(targetId)) throw badRequest("a player cannot be their own target");
        return db.social.deleteFriendship(playerId, targetId) > 0;
    }

    @Override
    public PaginatedList<FriendRequest> incomingFriendRequests(
        UUID playerId,
        int page,
        int pageSize
    ) {
        var rows = db.social.getIncomingFriendRequests(
            playerId,
            offset(page, pageSize),
            limit(pageSize)
        );
        return PaginatedList.of(
            rows,
            SocialQueries.GetIncomingFriendRequestsRow::totalCount,
            row -> new FriendRequest(Players.stub(row.player()), row.sentAt())
        );
    }

    @Override
    public PaginatedList<FriendRequest> outgoingFriendRequests(
        UUID playerId,
        int page,
        int pageSize
    ) {
        var rows = db.social.getOutgoingFriendRequests(
            playerId,
            offset(page, pageSize),
            limit(pageSize)
        );
        return PaginatedList.of(
            rows,
            SocialQueries.GetOutgoingFriendRequestsRow::totalCount,
            row -> new FriendRequest(Players.stub(row.player()), row.sentAt())
        );
    }

    @Override
    public FriendRequestResult sendFriendRequest(UUID playerId, UUID targetId) {
        if (playerId.equals(targetId)) throw badRequest("a player cannot be their own target");

        return db.txResult(tx -> {
            var sender = tx.players.getPlayerById(playerId);
            if (sender == null) throw notFound("no player " + playerId);
            var receiver = tx.players.getPlayerById(targetId);
            if (receiver == null) throw notFound("no player " + targetId);

            if (tx.social.isFriend(playerId, targetId)) {
                return new FriendRequestResult.AlreadyFriends();
            }

            var usage = Objects.requireNonNull(tx.social.getFriendUsage(playerId));
            var used = usage.friendCount() + usage.outgoingRequestCount();
            var limit = Roles.hasExtendedLimits(sender.role(), sender.hypercubeEnd())
                ? EXTENDED_FRIEND_LIMIT
                : FREE_FRIEND_LIMIT;
            if (used >= limit) {
                return new FriendRequestResult.LimitReached(
                    limit,
                    usage.friendCount(),
                    usage.outgoingRequestCount()
                );
            }

            if (tx.social.hasFriendRequest(targetId, playerId)) {
                // They asked first: this is the acceptance. Their request goes, and with it the
                // notification it left in this playerId's inbox.
                tx.social.insertFriendship(playerId, targetId);
                tx.social.deleteFriendRequests(targetId, playerId, false);
                notifications.deleteByKey(tx, playerId, FRIEND_REQUEST, targetId.toString());
                // Expiring at once, as Go's: the toast is the point, not the inbox row.
                var now = Instant.now();
                notifications.create(
                    tx,
                    targetId,
                    FRIEND_ADDED,
                    playerId.toString(),
                    null,
                    now,
                    false
                );
                notifications.create(
                    tx,
                    playerId,
                    FRIEND_ADDED,
                    targetId.toString(),
                    null,
                    now,
                    false
                );
                return new FriendRequestResult.Accepted();
            }

            if (autoRejects(receiver)) return new FriendRequestResult.TargetAutoRejects();

            if (tx.social.isBlocked(playerId, targetId)) {
                return new FriendRequestResult.BlockedTarget();
            }
            if (tx.social.isBlocked(targetId, playerId)) {
                return new FriendRequestResult.BlockedByTarget();
            }

            if (tx.social.insertFriendRequest(playerId, targetId) == 0) {
                return new FriendRequestResult.AlreadyRequested();
            }

            notifications.create(
                tx,
                targetId,
                FRIEND_REQUEST,
                playerId.toString(),
                null,
                null,
                true
            );
            return new FriendRequestResult.Sent();
        });
    }

    @Override
    public @Nullable FriendRequest deleteFriendRequest(
        UUID playerId,
        UUID targetId,
        boolean bidirectional
    ) {
        if (playerId.equals(targetId)) throw badRequest("a player cannot be their own target");

        return db.txResult(tx -> {
            var deleted = tx.social.deleteFriendRequests(playerId, targetId, bidirectional);
            if (deleted.isEmpty()) return null;
            var other = tx.players.getPlayerById(targetId);
            if (other == null) throw notFound("no player " + targetId);
            clearRequestNotifications(tx, deleted);
            return new FriendRequest(Players.stub(other), deleted.getFirst().createdAt());
        });
    }

    @Override
    public BlockResult block(UUID playerId, UUID targetId) {
        if (playerId.equals(targetId)) throw badRequest("a player cannot be their own target");

        return db.txResult(tx -> {
            var blocked = tx.players.getPlayerById(targetId);
            if (blocked == null) throw notFound("no player " + targetId);
            if (Roles.isStaff(blocked.role(), blocked.hypercubeEnd())) {
                return new BlockResult.TargetIsStaff();
            }
            if (tx.social.isBlocked(playerId, targetId)) return new BlockResult.AlreadyBlocked();

            tx.social.deleteFriendship(playerId, targetId);
            var requests = tx.social.deleteFriendRequests(playerId, targetId, true);
            clearRequestNotifications(tx, requests);
            tx.social.insertBlock(playerId, targetId);
            return new BlockResult.Blocked();
        });
    }

    @Override
    public boolean unblock(UUID playerId, UUID targetId) {
        if (playerId.equals(targetId)) throw badRequest("a player cannot be their own target");
        return db.social.deleteBlock(playerId, targetId) > 0;
    }

    @Override
    public PaginatedList<Block> blocks(UUID playerId, int page, int pageSize) {
        var rows = db.social.getBlocks(playerId, offset(page, pageSize), limit(pageSize));
        return PaginatedList.of(
            rows,
            SocialQueries.GetBlocksRow::totalCount,
            row -> new Block(playerId, Players.stub(row.player()), row.blockedAt())
        );
    }

    @Override
    public List<Block> blocksBetween(UUID playerId, UUID targetId, boolean bidirectional) {
        if (playerId.equals(targetId)) throw badRequest("a player cannot be their own target");
        return db.social.getBlocksBetween(playerId, targetId, bidirectional)
            .stream()
            .map(row -> new Block(row.blockerId(), Players.stub(row.player()), row.blockedAt()))
            .toList();
    }

    private void clearRequestNotifications(ApiDatabase.Tx tx, List<PlayerFriendRequests> deleted) {
        for (var row : deleted) {
            notifications.deleteByKey(
                tx,
                row.targetId(),
                FRIEND_REQUEST,
                row.playerId().toString()
            );
        }
    }

    private static boolean autoRejects(PlayerData receiver) {
        return Json.object(receiver.settings()).get(AUTO_REJECT_SETTING)
            instanceof JsonPrimitive value
            && value.isBoolean()
            && value.getAsBoolean();
    }

}
