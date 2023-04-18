package net.hollowcube.chat.command;

import net.hollowcube.chat.ChatFacet;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentEntity;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class UnmuteCommand extends Command {

    private final ArgumentEntity players = ArgumentType.Entity("players").onlyPlayers(true);

    private final ChatFacet chat;
    public UnmuteCommand(ChatFacet chat) {
        super("unmute");
        this.chat = chat;

        addSyntax(this::unmutePlayers, players);

        setDefaultExecutor((sender, context) -> sender.sendMessage("Usage: /unmute <players>"));
    }

    private void unmutePlayers(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (context.has(players)) {
            List<Entity> entities = context.get(players).find(sender);
            for (Entity entity : entities) {
                if (entity instanceof Player player) {
                    chat.handlePlayerUnmute(player);
                }
            }
        }
    }
}
