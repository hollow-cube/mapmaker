package net.hollowcube.mapmaker.command.relationship.friend;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.ipc.player.PlayerData;
import net.hollowcube.ipc.player.PlayerService;
import net.hollowcube.ipc.player.SocialService;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FriendRemoveCommand extends CommandDsl {
    private final Argument<@Nullable PlayerData> targetArg;

    private final SocialService social;

    public FriendRemoveCommand(@NotNull PlayerService players, @NotNull SocialService social) {
        super("remove");
        this.social = social;

        this.targetArg = CoreArgument.AnyPlayerData("target", players)
            .description("The friend to remove");

        this.addSyntax(playerOnly(this::exec), this.targetArg);
    }

    private void exec(@NotNull Player player, @NotNull CommandContext context) {
        var targetData = context.get(this.targetArg);
        if (targetData == null) return;
        if (targetData.id().equals(player.getUuid())) {
            player.sendMessage(Component.translatable("command.friend.remove.self"));
            return;
        }

        var targetDisplayName = targetData.displayName().render();

        if (this.social.removeFriend(player.getUuid(), targetData.id())) {
            player.sendMessage(Component.translatable("command.friend.remove.success", targetDisplayName));
        } else {
            player.sendMessage(Component.translatable("command.friend.remove.not_friends", targetDisplayName));
        }
    }
}
