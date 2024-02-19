package net.hollowcube.mapmaker.command.util;

import com.google.inject.Inject;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.chat.ChatMessageListener;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.temp.ClientChatMessageData;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ReplyCommand extends CommandDsl {
    private final Argument<String> messageArg = Argument.GreedyString("message");

    private final ChatMessageListener messageListener;

    @Inject
    public ReplyCommand(@NotNull ChatMessageListener messageListener) {
        super("reply", "r");
        this.messageListener = messageListener;

        addSyntax(playerOnly(this::handleReplyDirectMessage), messageArg);
    }

    private void handleReplyDirectMessage(@NotNull Player player, @NotNull CommandContext context) {
        var message = context.get(messageArg);


        message = FontUtil.stripInvalidChars(message).trim();
        if (message.isEmpty()) return;

        var currentMap = MapWorld.forPlayerOptional(player);
        if ((currentMap == null || !currentMap.map().isPublished()) && message.contains("[map]")) {
            player.sendMessage(Component.text("You are not in a published map.")); //todo message
            return;
        }

        var playerId = PlayerDataV2.fromPlayer(player).id();
        messageListener.sendChatMessage(new ClientChatMessageData(
                ClientChatMessageData.Type.CHAT_UNSIGNED,
                playerId, message, ClientChatMessageData.CHANNEL_REPLY,
                currentMap == null ? null : currentMap.map().id()
        ));
    }
}
