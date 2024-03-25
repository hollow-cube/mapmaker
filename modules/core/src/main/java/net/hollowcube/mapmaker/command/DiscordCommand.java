package net.hollowcube.mapmaker.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DiscordCommand extends CommandDsl {
    public DiscordCommand() {
        super("discord");

        this.description = "Gives you an invite link to join our Discord Server";
        this.category = CommandCategories.GLOBAL;

        addSyntax(playerOnly(this::giveDiscordLink));
    }

    public void giveDiscordLink(@NotNull Player player, @NotNull CommandContext context) {
        player.sendMessage(Component.translatable("command.discord"));
    }
}
