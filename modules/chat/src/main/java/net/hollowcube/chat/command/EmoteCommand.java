package net.hollowcube.chat.command;

import net.hollowcube.chat.ChatFacet;
import net.hollowcube.chat.Emote;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class EmoteCommand extends Command {

    private final Argument<String> emoteArg = ArgumentType.String("emote");

    private final ChatFacet chat;

    public String selectedEmote;
    public String emoteUnicode;

    public EmoteCommand(@NotNull ChatFacet chat) {
        super("emote", "emoji");
        this.chat = chat;

        addSyntax(this::sendEmojiChatMessage, emoteArg);

        setDefaultExecutor((sender, context) -> sender.sendMessage("Usage: /emoji <message>"));
    }

    private void sendEmojiChatMessage(@NotNull CommandSender sender, @NotNull CommandContext context) {

        Player player = (Player) sender;

        // Check if the player has the "staff" permission
//        if (!player.hasPermission("staff")) {
//            player.sendMessage(Component.translatable("command.generic.no_permission"));
//            return;
//        }

        // Ensure the staff chat message has content
        var emote = emoteUnicode;

        switch (context.get(emoteArg).toLowerCase()) {
            case "skull":
                selectedEmote = Emote.SKULL;
                emoteUnicode = Emote.SKULL_UNICODE;
                break;
            case "joy":
                selectedEmote = Emote.JOY;
                emoteUnicode = Emote.JOY_UNICODE;
                break;
            default:
                player.sendMessage("Unknown emote. Valid emotes are 'skull' or 'joy'.");
                return;
        }
        emote = emoteUnicode;
        chat.sendEmojiChatMessage(player, emote);
    }
}