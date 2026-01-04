package net.hollowcube.mapmaker.command.relationship.friend;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.player.DisplayName;
import net.hollowcube.mapmaker.player.PlayerFriend;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.session.Presence;
import net.hollowcube.mapmaker.session.SessionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class FriendListCommand extends CommandDsl {
    private final Argument<Integer> pageArg = Argument.Int("page").min(1).defaultValue(1);

    private final PlayerService playerService;
    private final MapService mapService;
    private final SessionManager sessionManager;

    public FriendListCommand(
        @NotNull PlayerService playerService, @NotNull MapService mapService, @NotNull SessionManager sessionManager) {
        super("list");
        this.playerService = playerService;
        this.mapService = mapService;
        this.sessionManager = sessionManager;

        this.addSyntax(playerOnly(this::exec));
        this.addSyntax(playerOnly(this::exec), this.pageArg);
    }

    private void exec(@NotNull Player player, @NotNull CommandContext context) {
        int page = context.get(this.pageArg);

        PlayerService.Page<PlayerFriend> friends = this.playerService.getPlayerFriends(player.getUuid().toString(),
                                                                                       new PlayerService.Pageable(page,
                                                                                                                  10));
        int pageCount = Math.ceilDiv(friends.totalItems(), 10);

        if (pageCount == 0) {
            player.sendMessage(Component.translatable("command.friend.list.empty"));
            return;
        }

        TextComponent.Builder builder = Component.text()
            .append(
                Component.translatable("command.friend.list.header", Component.text(page), Component.text(pageCount)));
        for (PlayerFriend friend : friends.items()) {
            DisplayName displayName = this.playerService.getPlayerDisplayName2(friend.playerId());
            Component username = displayName.asComponent();
            if (friend.online()) {
                Presence presence = this.sessionManager.getPresence(friend.playerId());
                builder.appendNewline().append(
                    switch (presence.type()) {
                        case Presence.TYPE_MAPMAKER_HUB ->
                            Component.translatable("command.friend.list.line.hub", username);
                        case Presence.TYPE_MAPMAKER_MAP -> {
                            var map = this.mapService.getMap(player.getUuid().toString(), presence.mapId());
                            if (Presence.MAP_BUILDING_STATES.contains(presence.state())) {
                                yield Component.translatable("command.friend.list.line.building", username,
                                                             Component.text(map.name()));
                            } else {
                                yield Component.translatable("command.friend.list.line.playing", username,
                                                             Component.text(map.name()));
                            }
                        }
                        default -> Component.translatable("command.friend.list.line.online_unknown", username);
                    });
            } else {
                builder.appendNewline()
                    .append(Component.translatable("command.friend.list.line.offline", displayName.asComponent()));
            }
        }
        player.sendMessage(builder.build());
    }
}
