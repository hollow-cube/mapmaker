package net.hollowcube.command;

import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface CommandExecutor {
    void execute(@NotNull CommandSender sender, @NotNull CommandContext context);

    @FunctionalInterface
    interface PlayerOnly {
        void execute(@NotNull Player player, @NotNull CommandContext context);
    }

}
