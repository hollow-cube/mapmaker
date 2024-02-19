package net.hollowcube.mapmaker.command.util;

import com.google.inject.Inject;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.chat.ChatMessageListener;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.session.SessionManager;
import net.hollowcube.mapmaker.temp.ClientChatMessageData;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MsgCommand extends CommandDsl {
    private final Argument<String> targetArg;
    private final Argument<String> messageArg = Argument.GreedyString("message");

    private final SessionManager sessionManager;
    private final MapService mapService;
    private final ChatMessageListener messageListener;

    @Inject
    public MsgCommand(@NotNull SessionManager sessionManager, @NotNull MapService mapService, @NotNull ChatMessageListener messageListener) {
        super("msg");
        this.sessionManager = sessionManager;
        this.mapService = mapService;
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

        var playerId = PlayerDataV2.fromPlayer(player).id();
        if (playerId.equals(target)) {
            player.sendMessage("You can't message yourself");
            return;
        }

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

        messageListener.sendChatMessage(new ClientChatMessageData(
                ClientChatMessageData.Type.CHAT_UNSIGNED,
                playerId, message, target, // Target is the channel id
                currentMapId
        ));
    }
}
