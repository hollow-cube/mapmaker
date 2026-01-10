package net.hollowcube.mapmaker.command.relationship.friend;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.player.responses.SendFriendRequestResult;
import net.kyori.adventure.text.Component;
import net.minestom.server.adventure.audience.Audiences;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FriendAddCommand extends CommandDsl {
    private final Argument<@Nullable PlayerData> targetArg;

    private final PlayerService playerService;

    public FriendAddCommand(@NotNull PlayerService playerService) {
        super("add");
        this.playerService = playerService;

        this.targetArg = CoreArgument.AnyPlayerData("target", playerService)
            .description("The player to add as a friend");

        addSyntax(playerOnly(this::exec), this.targetArg);
    }

    private void exec(@NotNull Player player, @NotNull CommandContext context) {
        var targetData = context.get(this.targetArg);
        if (targetData == null) return;
        if (targetData.id().equals(player.getUuid().toString())) {
            player.sendMessage(Component.translatable("command.friend.add.self"));
            return;
        }

        SendFriendRequestResult result = this.playerService.sendFriendRequest(player.getUuid().toString(),
                                                                              targetData.id());
        Component targetDisplayName = playerService.getPlayerDisplayName2(targetData.id()).build();

        Audiences.all().sendMessage(Component.text("debug result %s".formatted(result.toString())));
        if (result.successful()) {
            if (result.isRequest()) {
                player.sendMessage(
                    Component.translatable("command.friend.add.request_sent", targetDisplayName));
            } else {
                player.sendMessage(
                    Component.translatable("command.friend.add.added", targetDisplayName));
            }
            return;
        }

        SendFriendRequestResult.LimitError limitError = result.limitError();
        if (limitError != null) {
            String translationKey = "command.friend.add.limit_reached.";
            translationKey = translationKey +  (PlayerData.fromPlayer(player).isHypercube() ? "hypercube" : "non_hypercube");
            player.sendMessage(
                Component.translatable(translationKey, Component.text(limitError.limit()),
                                       Component.text(limitError.friendCount()),
                                       Component.text(limitError.outgoingRequestCount())));
            return;
        }

        if (result.error() == null) {
            player.sendMessage(Component.translatable("generic.unknown_error"));
            return;
        }

        switch (result.error().code()) {
            case "already_friends" -> player.sendMessage(
                Component.translatable("command.friend.add.already_friends", targetDisplayName));
            case "player_blocked" -> player.sendMessage(
                Component.translatable("command.friend.add.blocked_by_self", targetDisplayName));
            case "blocked_by_player", "target_auto_rejects_friend_requests" -> player.sendMessage(
                Component.translatable("command.friend.add.auto_rejected", targetDisplayName));
            case "friend_request_already_exists" -> player.sendMessage(
                Component.translatable("command.friend.add.already_requested", targetDisplayName));
            case "feature_disabled" -> player.sendMessage(Component.translatable("command.friend.add.feature_disabled_target", targetDisplayName));
            default -> player.sendMessage(Component.translatable("generic.unknown_error"));
        }
    }
}
