package net.hollowcube.mapmaker.command.util;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.session.Presence;
import net.hollowcube.mapmaker.session.SessionManager;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class WhereCommand extends CommandDsl {
    private final Argument<String> targetArg;

    private final ApiClient api;
    private final SessionManager sessionManager;
    private final PlayerService playerService;
    private final MapService mapService;

    public WhereCommand(
        @NotNull ApiClient api,
        @NotNull SessionManager sessionManager,
        @NotNull PlayerService playerService,
        @NotNull MapService mapService
    ) {
        super("where", "find");
        this.api = api;
        this.sessionManager = sessionManager;
        this.playerService = playerService;
        this.mapService = mapService;

        this.description = "Shows where someone is on the server";
        this.category = CommandCategories.SOCIAL;

        targetArg = CoreArgument.AnyOnlinePlayer("player", sessionManager)
            .description("The player you want to find");

        addSyntax(playerOnly(this::handleFindPlayer), targetArg);
    }

    private void handleFindPlayer(@NotNull Player player, @NotNull CommandContext context) {
        var senderId = player.getUuid().toString();

        // If the target was not specified, use the player themselves.
        var target = context.has(targetArg) ? context.get(targetArg) : senderId;
        if (target == null) {
            player.sendMessage(Component.translatable("generic.player.offline", Component.text(context.getRaw(targetArg))));
            return;
        }
        var presence = sessionManager.getPresence(target);
        if (presence == null) {
            player.sendMessage(Component.translatable("generic.player.offline", Component.text(context.getRaw(targetArg))));
            return;
        }

        // If checking self, just say where you are.
        if (senderId.equals(target)) {
            switch (presence.type()) {
                case Presence.TYPE_MAPMAKER_HUB -> player.sendMessage(Component.translatable("command.where.self.hub"));
                case Presence.TYPE_MAPMAKER_MAP -> {
                    var map = api.maps.get(presence.mapId());
                    if (Presence.MAP_BUILDING_STATES.contains(presence.state())) {
                        player.sendMessage(Component.translatable("command.where.self.building", Component.text(map.name())));
                    } else if (Presence.VERIFYING_STATE.equals(presence.state())) {
                        player.sendMessage(Component.translatable("command.where.self.verifying", Component.text(map.name())));
                    } else {
                        player.sendMessage(Component.translatable("command.where.self.playing", Component.text(map.name())));
                    }
                }
                default -> player.sendMessage(Component.translatable("command.where.self.unknown"));
            }
            return;
        }

        var targetName = playerService.getPlayerDisplayName2(target).build();
        switch (presence.type()) {
            case Presence.TYPE_MAPMAKER_HUB ->
                player.sendMessage(Component.translatable("command.where.hub", targetName));
            case Presence.TYPE_MAPMAKER_MAP -> {
                var senderPresence = Objects.requireNonNull(sessionManager.getPresence(senderId));
                if (senderPresence.mapId().equals(presence.mapId())) {
                    player.sendMessage(Component.translatable("command.where.same_map", targetName));
                    return;
                }

                var map = api.maps.get(presence.mapId());
                if (Presence.MAP_BUILDING_STATES.contains(presence.state())) {
                    player.sendMessage(Component.translatable("command.where.building", targetName, Component.text(target), Component.text(map.name())));
                } else if (Presence.VERIFYING_STATE.equals(presence.state())) {
                    player.sendMessage(Component.translatable("command.where.verifying", targetName));
                } else {
                    player.sendMessage(Component.translatable("command.where.playing", targetName, Component.text(target), Component.text(map.name())));
                }
            }
            default -> player.sendMessage(Component.translatable("command.where.unknown", targetName));
        }
    }
}
