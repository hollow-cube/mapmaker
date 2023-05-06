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

public class StaffChatCommand extends Command {

    private final Argument<String[]> messageArg = ArgumentType.StringArray("message");

    private final ChatFacet chat;

    public StaffChatCommand(@NotNull ChatFacet chat) {
        super("sc", "staffchat");
        this.chat = chat;

        addSyntax(this::sendStaffChatMessage, messageArg);

        setDefaultExecutor((sender, context) -> sender.sendMessage("Usage: /sc <message>"));
    }

    private void sendStaffChatMessage(@NotNull CommandSender sender, @NotNull CommandContext context) {

        Player player = (Player) sender;

        // Check if the player has the "staff" permission
//        if (!player.hasPermission("staff")) {
//            player.sendMessage(Component.translatable("command.generic.no_permission"));
//            return;
//        }

        // Ensure the staff chat message has content
        var message = String.join(" ", context.get(messageArg));
        if (message.isEmpty()) {
            player.sendMessage(Component.translatable("command.generic.chat_channel.no_message"));
            return;
        }

        chat.sendStaffChatMessage(player, message);
    }
}
