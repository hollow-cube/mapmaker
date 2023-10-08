package net.hollowcube.command;

import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface CommandExecutor {
    void execute(@NotNull CommandSender sender, @NotNull CommandContext context);

    @FunctionalInterface
    interface Player {
        void execute(@NotNull net.minestom.server.entity.Player player, @NotNull CommandContext context);
    }

}
