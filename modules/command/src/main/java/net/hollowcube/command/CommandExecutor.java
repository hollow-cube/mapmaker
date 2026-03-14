package net.hollowcube.command;

import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;

@FunctionalInterface
public interface CommandExecutor {
    void execute(CommandSender sender, CommandContext context);

    default void executeNextTick(CommandSender sender, CommandContext context) {
        if (sender instanceof Player player) {
            player.scheduleNextTick(_ -> execute(sender, context));
        } else {
            MinecraftServer.getSchedulerManager().scheduleNextTick(() -> execute(sender, context));
        }
    }

    @FunctionalInterface
    interface PlayerOnly {
        void execute(Player player, CommandContext context);
    }

}
