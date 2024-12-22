package net.hollowcube.mapmaker.command.util;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.session.SessionManager;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

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
    }

    private void handleListPlayers(@NotNull Player player, @NotNull CommandContext context) {
        var builder = Component.text();

        var sessions = List.copyOf(sessionManager.sessions(false));

        builder.append(Component.text("Players (" + sessions.size() + "): "));

        for (int i = 0; i < sessions.size(); i++) {
            var session = sessions.get(i);
            var displayName = playerService.getPlayerDisplayName2(session.playerId());
            builder.append(displayName.build());
            if (i != sessions.size() - 1) {
                builder.append(Component.text(", "));
            }
        }

        player.sendMessage(builder);
    }
}
