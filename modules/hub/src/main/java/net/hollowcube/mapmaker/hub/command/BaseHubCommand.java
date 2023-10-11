package net.hollowcube.mapmaker.hub.command;

import net.kyori.adventure.text.Component;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.CommandExecutor;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

/**
 * A command which is only available in a mapmaker hub world.
 */
public class BaseHubCommand extends Command {
    public BaseHubCommand(@NotNull String name, @Nullable String... aliases) {
        super(name, aliases);

        setDefaultExecutor((sender, context) -> sender.sendMessage("todo: help"));
    }

    protected @NotNull CommandExecutor wrap(@NotNull BiConsumer<@NotNull Player, @NotNull CommandContext> executor) {
        return (sender, context) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.translatable("generic.players_only"));
                return;
            }

            executor.accept(player, context);
        };
    }
}
