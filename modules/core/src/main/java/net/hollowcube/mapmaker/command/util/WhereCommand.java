package net.hollowcube.mapmaker.command.util;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.mapmaker.command.CommandCategory;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.session.Presence;
import net.hollowcube.mapmaker.session.SessionManager;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class WhereCommand extends Command {
    private final Argument<String> targetArg;

    private final SessionManager sessionManager; // Optional for backwards compatibility
    private final PlayerService playerService;
    private final MapService mapService;

    public WhereCommand(@NotNull SessionManager sessionManager, @NotNull PlayerService playerService, @NotNull MapService mapService) {
        super("where", "find");
        this.sessionManager = sessionManager;
        this.playerService = playerService;
        this.mapService = mapService;

        category = CommandCategory.SOCIAL;

        targetArg = CoreArgument.AnyOnlinePlayer("player", sessionManager);

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
                    var map = mapService.getMap(senderId, presence.mapId());
                    if (Presence.MAP_BUILDING_STATES.contains(presence.state())) {
                        player.sendMessage(Component.translatable("command.where.self.building", Component.text(map.name())));
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
                } else if (Presence.MAP_BUILDING_STATES.contains(presence.state())) {
                    player.sendMessage(Component.translatable("command.where.building", targetName));
                } else {
                    player.sendMessage(Component.translatable("command.where.playing", targetName));
                }
            }
            default -> player.sendMessage(Component.translatable("command.where.unknown", targetName));
        }
    }
}
