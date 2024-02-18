package net.hollowcube.mapmaker.command.util;

import com.google.inject.Inject;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.chat.ChatMessageListener;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.session.SessionManager;
import net.hollowcube.mapmaker.temp.ClientChatMessageData;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MsgCommand extends CommandDsl {
    private final Argument<String> targetArg;
    private final Argument<String> messageArg = Argument.GreedyString("message");

    private final ChatMessageListener messageListener;

    @Inject
    public MsgCommand(@NotNull SessionManager sessionManager, @NotNull ChatMessageListener messageListener) {
        super("msg");
        this.messageListener = messageListener;

        this.targetArg = CoreArgument.AnyOnlinePlayer("player", sessionManager);

        addSyntax(playerOnly(this::handleSendDirectMessage), targetArg, messageArg);
    }

    private void handleSendDirectMessage(@NotNull Player player, @NotNull CommandContext context) {
        var target = context.get(targetArg);
        var message = context.get(messageArg);

        if (target == null) {
            player.sendMessage("Player not online");
            return;
        }

        message = FontUtil.stripInvalidChars(message).trim();
        if (message.isEmpty()) return;

        var playerId = PlayerDataV2.fromPlayer(player).id();
        messageListener.sendChatMessage(new ClientChatMessageData(
                ClientChatMessageData.Type.CHAT_UNSIGNED,
                // Target is the channel id
                playerId, message, target, null
                //todo set the map
        ));
    }
}
