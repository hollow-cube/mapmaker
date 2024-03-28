package net.hollowcube.mapmaker.command.util;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PingCommand extends CommandDsl {
    public PingCommand() {
        super("ping");

        category = CommandCategories.GLOBAL;
        description = "Shows your latency to the server";

        addSyntax(playerOnly(this::handlePing));
    }

    private void handlePing(@NotNull Player player, @NotNull CommandContext context) {
        player.sendMessage("latency is " + player.getLatency() + "ms");
    }

}
