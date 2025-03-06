package net.hollowcube.mapmaker.command.chat;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.mapmaker.chat.ChatMessageListener;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.session.SessionManager;
import net.hollowcube.mapmaker.temp.ClientChatMessageData;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ReplyCommand extends AbstractChatCommand {
    private final Argument<String> messageArg = Argument.GreedyString("message")
            .description("The message content to send in your reply");

    public ReplyCommand(@NotNull SessionManager sessions, @NotNull MapService maps, @NotNull ChatMessageListener messages) {
        super(sessions, maps, messages, "reply", "r");

        this.description = "Sends a reply to the last player that messaged you";
        this.category = CommandCategories.SOCIAL;

        addSyntax(playerOnly(this::handleReplyDirectMessage), messageArg);
    }

    private void handleReplyDirectMessage(@NotNull Player player, @NotNull CommandContext context) {
        this.handle(player, ClientChatMessageData.CHANNEL_REPLY, context.get(messageArg));
    }
}
