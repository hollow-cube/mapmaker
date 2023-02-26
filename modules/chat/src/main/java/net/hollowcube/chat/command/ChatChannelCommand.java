package net.hollowcube.chat.command;

import net.hollowcube.chat.ChatFacet;
import net.kyori.adventure.text.Component;
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

        // Ensure the player is hooking into the right channel
        var chatChannel = context.get(channelType);
        if (chatChannel.contains("s") || chatChannel.contains("staff")) {
            //if (player.hasPermission("staff")) {
                chat.isStaffChatChannel = true;
                player.sendMessage("You have switched to the staff chat channel.");
            //} else {
                //player.sendMessage(Component.translatable("command.generic.no_permission"));
            //}
        } else {
            if (chatChannel.contains("g") || chatChannel.contains("global")) {
                chat.isStaffChatChannel = false;
                player.sendMessage("You have switched to the global chat channel.");
            } else {
                player.sendMessage("That is not a valid chat channel.");
            }
        }
    }
    //TODO finish some logic here and in chatfacet for handling the chat, and remove the silly boolean and if statements for better alternatives because it's lazy and dumb :))))
}