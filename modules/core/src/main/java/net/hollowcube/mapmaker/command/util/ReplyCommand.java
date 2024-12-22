package net.hollowcube.mapmaker.command.util;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.chat.ChatMessageListener;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.session.SessionManager;
import net.hollowcube.mapmaker.temp.ClientChatMessageData;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

public class ReplyCommand extends CommandDsl {
    private final Argument<String> messageArg = Argument.GreedyString("message")
            .description("The message content to send in your reply");

    private final SessionManager sessionManager;
    private final MapService mapService;
    private final ChatMessageListener messageListener;

    public ReplyCommand(@NotNull SessionManager sessionManager, @NotNull MapService mapService, @NotNull ChatMessageListener messageListener) {
        super("reply", "r");
        this.sessionManager = sessionManager;
        this.mapService = mapService;
        this.messageListener = messageListener;

        this.description = "Sends a reply to the last player that messaged you";
        this.category = CommandCategories.SOCIAL;

        addSyntax(playerOnly(this::handleReplyDirectMessage), messageArg);
    }

    private void handleReplyDirectMessage(@NotNull Player player, @NotNull CommandContext context) {
        var message = context.get(messageArg);

        message = FontUtil.stripInvalidChars(message).trim();
        if (message.isEmpty()) return;

        String currentMapId = null;
        if (message.contains("[map]")) {
            var currentMap = MiscFunctionality.getCurrentMap(sessionManager, mapService, player);
            if (currentMap == null || !currentMap.isPublished()) {
                player.sendMessage(Component.text("You are not in a published map.")); //todo message
                return;
            }
            currentMapId = currentMap.id();
        }

        var playerId = PlayerDataV2.fromPlayer(player).id();
        long messageSeed = ThreadLocalRandom.current().nextLong();
        messageListener.trySendChatMessage(player, new ClientChatMessageData(
                ClientChatMessageData.Type.CHAT_UNSIGNED,
                playerId, message, ClientChatMessageData.CHANNEL_REPLY,
                currentMapId, messageSeed
        ));
    }
}
