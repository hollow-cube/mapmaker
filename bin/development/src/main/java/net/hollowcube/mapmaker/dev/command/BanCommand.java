package net.hollowcube.mapmaker.dev.command;

import net.hollowcube.mapmaker.storage.BanStorage;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentEntity;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BanCommand extends Command {
    private final ArgumentEntity players = ArgumentType.Entity("players").onlyPlayers(true);

    private final BanStorage bans;
    public BanCommand(BanStorage bans) {
        super("ban");
        this.bans = bans;

        addSyntax(this::banPlayers, players);

        setDefaultExecutor((sender, context) -> sender.sendMessage("Usage: /ban <players>"));
    }

    private void banPlayers(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (context.has(players)) {
            List<Entity> entities = context.get(players).find(sender);
            for (Entity entity : entities) {
                if (entity instanceof Player player) {
                    bans.banPlayer(player);
                }
            }
        }
    }
}
