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
    public String emoteUnicode;

    public EmoteCommand(@NotNull ChatFacet chat) {
        super("emote", "emoji");
        this.chat = chat;

        addSyntax(this::sendEmojiChatMessage, emoteArg);

        setDefaultExecutor((sender, context) -> sender.sendMessage("Usage: /emoji <message>"));
    }

    private void sendEmojiChatMessage(@NotNull CommandSender sender, @NotNull CommandContext context) {

        Player player = (Player) sender;

        switch (context.get(emoteArg).toLowerCase()) {
            case Emote.SKULL_CMD:
                emoteUnicode = Emote.SKULL_UNICODE;
                break;
            case Emote.JOY_CMD:
                emoteUnicode = Emote.JOY_UNICODE;
                break;
            default:
                player.sendMessage("Unknown emote. Valid emotes are 'skull' or 'joy'.");
                return;
        }
        chat.sendEmojiChatMessage(player, emoteUnicode);
    }
}