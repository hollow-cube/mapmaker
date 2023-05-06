package net.hollowcube.chat.command;

import net.hollowcube.chat.ChatFacet;
import net.hollowcube.chat.ChatMessage;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ChatChannelCommand extends Command {

    private final Argument<String> channelType = ArgumentType.String("channel");

    private final ChatFacet chat;

    public ChatChannelCommand(@NotNull ChatFacet chat) {
        super("chat");
        this.chat = chat;

        addSyntax(this::chatChannel, channelType);

        setDefaultExecutor((sender, context) -> sender.sendMessage("Usage: /chat <channel>"));
    }

    private void chatChannel(@NotNull CommandSender sender, @NotNull CommandContext context) {
        Player player = (Player) sender;

        // Check if the player has the "staff" permission
//        if (!player.hasPermission("staff")) {
//            player.sendMessage(Component.translatable("command.generic.no_permission"));
//            return;
//        }

        String selectedChannel = ChatMessage.DEFAULT_CONTEXT;

        switch (context.get(channelType).toLowerCase()) {
            case "s":
            case "staff":
                selectedChannel = ChatMessage.STAFF_CONTEXT;
                break;
            case "g":
            case "global":
                break;
            default:
                player.sendMessage("Unknown channel \"" + context.get(channelType) + "\", using global chat.");
                break;
        }

        player.sendMessage("You have switched to the " + selectedChannel + " chat channel.");
        player.setTag(ChatFacet.CHAT_CHANNEL, selectedChannel);
    }
}