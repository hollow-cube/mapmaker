package net.hollowcube.mapmaker.command.relationship.friend;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.player.PlayerFriend;
import net.hollowcube.mapmaker.player.PlayerService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;

public class FriendListCommand extends CommandDsl {

    private final PlayerService playerService;

    public FriendListCommand(@NotNull PlayerService playerService) {
        super("list");
        this.playerService = playerService;

        addSyntax(playerOnly(this::exec));
    }

    private void exec(@NotNull Player player, @NotNull CommandContext context) {
        List<PlayerFriend> friends = this.playerService.getPlayerFriends(player.getUuid().toString());

        TextComponent.Builder builder = Component.text().append(Component.translatable("command.friend.list.header"));
        for (PlayerFriend friend : friends) {
            builder.appendNewline().append(Component.translatable("command.friend.list.line", friend.username()));
        }
        player.sendMessage(builder.build());
    }
}
