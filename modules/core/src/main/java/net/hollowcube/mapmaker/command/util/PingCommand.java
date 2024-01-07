package net.hollowcube.mapmaker.command.util;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PingCommand extends CommandDsl {
    public PingCommand() {
        super("ping");

        addSyntax(playerOnly(this::handlePing));
    }

    private void handlePing(@NotNull Player player, @NotNull CommandContext context) {
        player.sendMessage("latency is " + (player.getLatency() - 50) + "±50ms");
    }

}
