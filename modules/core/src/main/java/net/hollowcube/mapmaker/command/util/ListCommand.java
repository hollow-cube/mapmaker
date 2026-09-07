package net.hollowcube.mapmaker.command.util;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.ipc.player.DisplayName;
import net.hollowcube.ipc.player.PlayerService;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.session.PlayerSession;
import net.hollowcube.mapmaker.session.SessionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

public class ListCommand extends CommandDsl {
    private final SessionManager sessionManager;
    private final PlayerService players;

    public ListCommand(@NotNull SessionManager sessionManager, @NotNull PlayerService players) {
        super("list");
        this.sessionManager = sessionManager;
        this.players = players;

        category = CommandCategories.SOCIAL;
        description = "Lists all players on the server";

        addSyntax(playerOnly(this::handleListPlayers));

        addSubcommand(new MapListCommand());
    }

    private void handleListPlayers(@NotNull Player player, @NotNull CommandContext context) {
        var sessions = List.copyOf(sessionManager.sessions(false));
        var ids = sessions.stream().map(session -> UUID.fromString(session.playerId())).toList();
        var names = players.displayNames(ids);
        var playerNames = ids.stream().map(names::get).map(DisplayName::render).toList();

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
            var ids = player.getInstance().getPlayers().stream()
                .map(otherPlayer -> otherPlayer.getUuid().toString())
                .filter(Predicate.not(sessionManager::isHidden))
                .map(UUID::fromString)
                .toList();
            var playerNames = players.displayNames(ids).values().stream().map(DisplayName::render).toList();

            var builder = Component.text();
            builder.append(Component.text("Map Players (" + playerNames.size() + "): "));
            builder.append(Component.join(JoinConfiguration.commas(true), playerNames));

            player.sendMessage(builder);
        }
    }
}
