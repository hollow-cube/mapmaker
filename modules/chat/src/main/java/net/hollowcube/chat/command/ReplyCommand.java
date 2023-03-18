package net.hollowcube.chat.command;

import net.hollowcube.chat.ChatFacet;
import net.hollowcube.common.lang.GenericMessages;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ReplyCommand extends Command {
    private final Argument<String[]> messageArg = ArgumentType.StringArray("message");

    private final ChatFacet chat;

    public ReplyCommand(@NotNull ChatFacet chat) {
        super("reply", "r");
        this.chat = chat;

        addSyntax(this::sendReplyMessage, messageArg);

        setDefaultExecutor((sender, context) -> sender.sendMessage("Usage: /reply <message>"));
    }

    private void sendReplyMessage(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(GenericMessages.COMMAND_PLAYER_ONLY);
            return;
        }

        var targetId = player.getTag(ChatFacet.REPLY_TO);
        if (targetId == null) {
            sender.sendMessage("You have no one to reply to.");
            return;
        }
        var target = MinecraftServer.getConnectionManager().getPlayer(targetId);
        if (target == null) {
            sender.sendMessage("The player you were replying to is no longer online."); // todo translatable
            return;
        }

        var message = String.join(" ", context.get(messageArg));
        if (message.isBlank()) {
            sender.sendMessage("empty message"); // todo translatable
            return;
        }

        chat.sendPrivateMessage(player, target, message);
    }
}
