package net.hollowcube.terraform.command.history;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.terraform.session.LocalSession;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ClearHistoryCommand extends CommandDsl {
    public ClearHistoryCommand() {
        super("clearhistory");

        addSyntax(playerOnly(this::handleClearHistory));
    }

    private void handleClearHistory(@NotNull Player player, @NotNull CommandContext context) {
        var session = LocalSession.forPlayer(player);
        session.clearHistory();
        player.sendMessage(Component.translatable("terraform.history.cleared"));
    }
}
