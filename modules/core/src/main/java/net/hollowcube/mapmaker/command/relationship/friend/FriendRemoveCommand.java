package net.hollowcube.mapmaker.command.relationship.friend;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.api.players.PlayerClient;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.player.PlayerService;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FriendRemoveCommand extends CommandDsl {
    private final Argument<@Nullable PlayerData> targetArg;

    private final PlayerClient players;
    private final PlayerService playerService;

    public FriendRemoveCommand(@NotNull PlayerClient players, @NotNull PlayerService playerService) {
        super("remove");
        this.players = players;
        this.playerService = playerService;

        this.targetArg = CoreArgument.AnyPlayerData("target", players)
            .description("The friend to remove");

        this.addSyntax(playerOnly(this::exec), this.targetArg);
    }

    private void exec(@NotNull Player player, @NotNull CommandContext context) {
        var targetData = context.get(this.targetArg);
        if (targetData == null) return;
        if (targetData.id().equals(player.getUuid().toString())) {
            player.sendMessage(Component.translatable("command.friend.remove.self"));
            return;
        }

        var targetDisplayName = players.getDisplayName(targetData.id()).build();

        try {
            this.playerService.removeFriend(player.getUuid().toString(), targetData.id());
            player.sendMessage(Component.translatable("command.friend.remove.success", targetDisplayName));
        } catch (PlayerService.NotFoundError ex) {
            player.sendMessage(Component.translatable("command.friend.remove.not_friends", targetDisplayName));
        }
    }
}
