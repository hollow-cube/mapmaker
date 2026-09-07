package net.hollowcube.mapmaker.command.relationship;

import net.hollowcube.ipc.player.FriendRequestResult;
import net.hollowcube.ipc.player.SocialService;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;

import static net.hollowcube.mapmaker.player.LocalPlayer.localPlayer;

import java.util.UUID;

public final class SocialUtil {
    public static boolean failIfBlocked(
        SocialService social,
        Player player,
        String targetId,
        String targetUsername,
        boolean bidirectional
    ) {
        var playerId = player.getUuid();
        var blocks = social.blocksBetween(UUID.fromString(targetId), playerId, bidirectional);
        if (blocks.isEmpty()) return false;
        var key = blocks.getFirst().blockerId().equals(playerId)
            ? "generic.command.blocked_by_self" : "generic.command.blocked_by_target";
        player.sendMessage(Component.translatable(key, Component.text(targetUsername)));
        return true;
    }

    public static Component friendRequestMessage(Player player, FriendRequestResult result, Component name) {
        return switch (result) {
            case FriendRequestResult.Sent _ -> Component.translatable("command.friend.add.request_sent", name);
            case FriendRequestResult.Accepted _ -> Component.translatable("command.friend.add.added", name);
            case FriendRequestResult.AlreadyFriends _ -> Component.translatable("command.friend.add.already_friends", name);
            case FriendRequestResult.AlreadyRequested _ -> Component.translatable("command.friend.add.already_requested", name);
            case FriendRequestResult.BlockedTarget _ -> Component.translatable("command.friend.add.blocked_by_self", name);
            case FriendRequestResult.BlockedByTarget _, FriendRequestResult.TargetAutoRejects _ ->
                Component.translatable("command.friend.add.auto_rejected", name);
            case FriendRequestResult.LimitReached limit -> Component.translatable(
                "command.friend.add.limit_reached." + (localPlayer(player).isHypercube() ? "hypercube" : "non_hypercube"),
                Component.text(limit.limit()), Component.text(limit.friendCount()), Component.text(limit.outgoingRequestCount()));
            case FriendRequestResult.Unknown _ -> Component.translatable("generic.unknown_error");
        };
    }

    private SocialUtil() {}
}
