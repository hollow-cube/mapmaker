package net.hollowcube.mapmaker.dev.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentEntity;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class KickCommand extends Command {

    ArgumentEntity players = ArgumentType.Entity("player").onlyPlayers(true);
    @NotNull Argument<String[]> reason = ArgumentType.StringArray("reason").setDefaultValue(new String[]{"Kicked from the server"});

    public KickCommand() {
        super("kick");

        addSyntax(this::kickPlayers, players);

        setDefaultExecutor((sender, context) -> sender.sendMessage("Usage: /kick [player] [reason]"));
    }

    private void kickPlayers(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (context.has(players)) {
            String kickReason = String.join(" ", context.get(reason));
            for(Entity entity : context.get(players).find(sender)) {
                if(entity instanceof Player player) {
                    player.kick(Component.text(kickReason, NamedTextColor.RED));
                }
            }
        }
    }
}
