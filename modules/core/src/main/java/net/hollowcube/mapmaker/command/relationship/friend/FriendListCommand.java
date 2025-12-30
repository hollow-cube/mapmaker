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

public class FriendListCommand extends CommandDsl {
    private final Argument<Integer> pageArg = Argument.Int("page").min(1).defaultValue(1);

    private final PlayerService playerService;

    public FriendListCommand(@NotNull PlayerService playerService) {
        super("list");
        this.playerService = playerService;

        this.addSyntax(playerOnly(this::exec));
        this.addSyntax(playerOnly(this::exec), this.pageArg);
    }

    private void exec(@NotNull Player player, @NotNull CommandContext context) {
        int page = context.get(this.pageArg);

        PlayerService.Page<PlayerFriend> friends = this.playerService.getPlayerFriends(player.getUuid().toString(), new PlayerService.Pageable(page, 10));
        int pageCount = Math.ceilDiv(friends.totalItems(), 10);

        TextComponent.Builder builder = Component.text().append(Component.translatable("command.friend.list.header", Component.text(page), Component.text(pageCount)));
        for (PlayerFriend friend : friends.items()) {
            builder.appendNewline().append(Component.translatable("command.friend.list.line", Component.text(friend.username())));
        }
        player.sendMessage(builder.build());
    }
}
