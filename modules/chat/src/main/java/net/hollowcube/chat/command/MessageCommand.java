package net.hollowcube.chat.command;

import net.hollowcube.chat.ChatFacet;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentEntity;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MessageCommand extends Command {
    private final ArgumentEntity playerArg = ArgumentType.Entity("player").onlyPlayers(true).singleEntity(true);
    private final Argument<String[]> messageArg = ArgumentType.StringArray("message");

    private final ChatFacet chat;

    public MessageCommand(@NotNull ChatFacet chat) {
        super("message", "msg");
        this.chat = chat;

        addSyntax(this::sendMessageToPlayer, playerArg, messageArg);

        setDefaultExecutor((sender, context) -> {
            sender.sendMessage("Usage: /msg <player> <message>");
        });
    }

    private void sendMessageToPlayer(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return;
        }

        var target = context.get(playerArg).findFirstPlayer(sender);
        var message = String.join(" ", context.get(messageArg));

        if (message.isBlank()) {
            sender.sendMessage("empty message");
            return;
        }

        if (target == null) {
            sender.sendMessage("player not found");
            return;
        }

        chat.sendPrivateMessage(player, target, message);
    }
}
