package net.hollowcube.terraform.command.helper;

import net.kyori.adventure.text.Component;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.CommandExecutor;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

public class TerraformCommand extends Command {
    public TerraformCommand(@NotNull String name, @Nullable String... aliases) {
        super(name, aliases);
    }

    public TerraformCommand(@NotNull String name) {
        super(name);
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
