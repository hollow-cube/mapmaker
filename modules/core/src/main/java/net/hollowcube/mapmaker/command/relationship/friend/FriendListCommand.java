package net.hollowcube.mapmaker.command.relationship.friend;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.lang.TimeComponent;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.ipc.player.SocialService;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.session.PlayerSession;
import net.hollowcube.mapmaker.session.Presence;
import net.hollowcube.mapmaker.session.SessionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class FriendListCommand extends CommandDsl {
    private final Argument<Integer> pageArg = Argument.Int("page").min(1).defaultValue(1);

    private final ApiClient api;
    private final SocialService social;
    private final SessionManager sessionManager;

    public FriendListCommand(
        @NotNull ApiClient api,
        @NotNull SocialService social,
        @NotNull SessionManager sessionManager
    ) {
        super("list");
        this.api = api;
        this.social = social;
        this.sessionManager = sessionManager;

        this.addSyntax(playerOnly(this::exec));
        this.addSyntax(playerOnly(this::exec), this.pageArg);
    }

    private void exec(@NotNull Player player, @NotNull CommandContext context) {
        int page = context.get(this.pageArg);

        var friends = this.social.friends(player.getUuid(), false, page - 1, 10);
        int pageCount = Math.ceilDiv(friends.count(), 10);
        
        if (pageCount == 0) {
            player.sendMessage(Component.translatable("command.friend.list.empty"));
            return;
        }

        TextComponent.Builder builder = Component.text()
            .append(
                Component.translatable("command.friend.list.header", Component.text(page), Component.text(pageCount)));
        for (var friend : friends.results()) {
            var name = friend.player().displayName().render();
            PlayerSession session = this.sessionManager.getSession(friend.player().id().toString());
            if (friend.online() && session != null && !session.hidden()) {
                Presence presence = session.presence();
                builder.appendNewline().append(
                    switch (OpUtils.map(presence, Presence::type)) {
                        case Presence.TYPE_MAPMAKER_HUB ->
                            Component.translatable("command.friend.list.line.hub", name);
                        case Presence.TYPE_MAPMAKER_MAP -> {
                            var map = api.maps.get(presence.mapId());
                            if (Presence.MAP_BUILDING_STATES.contains(presence.state())) {
                                yield Component.translatable("command.friend.list.line.building", name,
                                                             Component.text(friend.player().username()),
                                                             Component.text(map.name()));
                            } else if (Presence.VERIFYING_STATE.equals(presence.state())) {
                                yield Component.translatable("command.friend.list.line.verifying", name,
                                                             Component.text(map.name()));
                            } else {
                                yield Component.translatable("command.friend.list.line.playing", name,
                                                             Component.text(friend.player().username()),
                                                             Component.text(map.name()));
                            }
                        }
                        case null, default -> Component.translatable("command.friend.list.line.online_unknown", name);
                    });
            } else {
                builder.appendNewline()
                    .append(Component.translatable("command.friend.list.line.offline", name,
                                                   TimeComponent.of(friend.lastOnline())));
            }
        }
        player.sendMessage(builder.build());
    }
}
