package net.hollowcube.mapmaker.dev.command;

import net.hollowcube.mapmaker.storage.BanStorage;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import org.jetbrains.annotations.NotNull;

public class UnbanCommand extends Command {

    private final BanStorage bans;
    public UnbanCommand(BanStorage bans) {
        super("unban", "pardon");
        this.bans = bans;

        setDefaultExecutor((sender, context) -> sender.sendMessage("Usage: /unban <players>"));
    }

    private void banPlayers(@NotNull CommandSender sender, @NotNull CommandContext context) {
        // TODO
    }
}
