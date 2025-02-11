package net.hollowcube.mapmaker.command.util;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.player.DisplayName;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.session.PlayerSession;
import net.hollowcube.mapmaker.session.SessionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Predicate;

public class ListCommand extends CommandDsl {
    private final SessionManager sessionManager;
    private final PlayerService playerService;

    public ListCommand(@NotNull SessionManager sessionManager, @NotNull PlayerService playerService) {
        super("list");
        this.sessionManager = sessionManager;
        this.playerService = playerService;

        category = CommandCategories.SOCIAL;
        description = "Lists all players on the server";

        addSyntax(playerOnly(this::handleListPlayers));

        addSubcommand(new MapListCommand());
    }

    private void handleListPlayers(@NotNull Player player, @NotNull CommandContext context) {
        var sessions = List.copyOf(sessionManager.sessions(false));
        var playerNames = sessions
                .stream()
                .map(PlayerSession::playerId)
                .map(playerService::getPlayerDisplayName2)
                .map(DisplayName::build)
                .toList();

        var builder = Component.text();
        builder.append(Component.text("Players (" + playerNames.size() + "): "));
        builder.append(Component.join(JoinConfiguration.commas(true), playerNames));

        player.sendMessage(builder);
    }

    private class MapListCommand extends CommandDsl {

        public MapListCommand() {
            super("map");

            category = CommandCategories.SOCIAL;
            description = "Lists all maps on the server";

            addSyntax(playerOnly(this::handleListMaps));
        }

        private void handleListMaps(@NotNull Player player, @NotNull CommandContext context) {
            var playerNames = player.getInstance().getPlayers()
                    .stream()
                    .map(otherPlayer -> otherPlayer.getUuid().toString())
                    .filter(Predicate.not(sessionManager::isHidden))
                    .map(playerService::getPlayerDisplayName2)
                    .map(DisplayName::build)
                    .toList();

            var builder = Component.text();
            builder.append(Component.text("Map Players (" + playerNames.size() + "): "));
            builder.append(Component.join(JoinConfiguration.commas(true), playerNames));

            player.sendMessage(builder);
        }
    }
}
