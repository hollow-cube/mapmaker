package net.hollowcube.mapmaker.command.chat;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.chat.ChatMessageListener;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.session.SessionManager;
import net.hollowcube.mapmaker.temp.ClientChatMessageData;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

public class MsgCommand extends AbstractChatCommand {

    private final Argument<String> targetArg;
    private final Argument<String> messageArg = CoreArgument.Message("message")
            .description("The message content to send");

    public MsgCommand(@NotNull SessionManager sessions, @NotNull MapService maps, @NotNull ChatMessageListener messages) {
        super(sessions, maps, messages, "msg");

        this.targetArg = CoreArgument.AnyOnlinePlayer("player", sessions)
                .description("The player to send the message to");

        this.description = "Send a direct message to a player";
        this.category = CommandCategories.SOCIAL;

        addSyntax(playerOnly(this::handleSendDirectMessage), targetArg, messageArg);
    }

    private void handleSendDirectMessage(@NotNull Player player, @NotNull CommandContext context) {
        var target = context.get(targetArg);
        var message = context.get(messageArg);

        if (target == null) {
            player.sendMessage(Component.translatable("generic.other_players_only"));
            return;
        }
        if (PlayerDataV2.fromPlayer(player).id().equals(target)) {
            player.sendMessage(Component.translatable("chat.msg.cant_message_yourself"));
            return;
        }

        this.handle(player, target, message);
    }
}
