package net.hollowcube.mapmaker.command.relationship.friend;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.ipc.player.FriendRequestResult;
import net.hollowcube.ipc.player.PlayerData;
import net.hollowcube.ipc.player.PlayerService;
import net.hollowcube.ipc.player.SocialService;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.hollowcube.mapmaker.player.LocalPlayer.localPlayer;

public class FriendAddCommand extends CommandDsl {
    private final Argument<@Nullable PlayerData> targetArg;

    private final SocialService social;

    public FriendAddCommand(@NotNull PlayerService players, @NotNull SocialService social) {
        super("add");
        this.social = social;

        this.targetArg = CoreArgument.AnyPlayerData("target", players)
            .description("The player to add as a friend");

        addSyntax(playerOnly(this::exec), this.targetArg);
    }

    private void exec(@NotNull Player player, @NotNull CommandContext context) {
        var targetData = context.get(this.targetArg);
        if (targetData == null) return;
        if (targetData.id().equals(player.getUuid())) {
            player.sendMessage(Component.translatable("command.friend.add.self"));
            return;
        }

        var result = this.social.sendFriendRequest(player.getUuid(), targetData.id());
        var name = targetData.displayName().render();
        player.sendMessage(switch (result) {
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
        });
    }
}
