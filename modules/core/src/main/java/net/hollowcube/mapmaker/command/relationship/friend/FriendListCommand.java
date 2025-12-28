package net.hollowcube.mapmaker.command.relationship.friend;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
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
    private final Argument<Integer> pageArg = Argument.Int("page").min(1).defaultValue(1);

    private final PlayerService playerService;

    public FriendListCommand(@NotNull PlayerService playerService) {
        super("list");
        this.playerService = playerService;

        addSyntax(playerOnly(this::exec));
        addSyntax(playerOnly(this::exec), this.pageArg);
    }

    private void exec(@NotNull Player player, @NotNull CommandContext context) {
        List<PlayerFriend> friends = this.playerService.getPlayerFriends(player.getUuid().toString());

        int pageLength = 10;
        int page = Math.min((friends.size() / pageLength) + 1, context.get(this.pageArg));
        int pageCount = Math.ceilDiv(friends.size(), pageLength);
        int start = (page - 1) * pageLength;

        friends = friends.subList(start, Math.min(start + pageLength, friends.size()));

        TextComponent.Builder builder = Component.text().append(Component.translatable("command.friend.list.header", Component.text(page), Component.text(pageCount)));
        for (PlayerFriend friend : friends) {
            builder.appendNewline().append(Component.translatable("command.friend.list.line", Component.text(friend.username())));
        }
        player.sendMessage(builder.build());
    }
}
