package net.hollowcube.mapmaker.command.chat;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.ipc.chat.ChatChannel;
import net.hollowcube.ipc.player.SocialService;
import net.hollowcube.mapmaker.chat.ChatMessageListener;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.command.relationship.SocialUtil;
import net.hollowcube.mapmaker.session.SessionManager;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.player.LocalPlayer.localPlayer;

public class MsgCommand extends AbstractChatCommand {

    private final Argument<String> targetArg;
    private final Argument<String> messageArg = CoreArgument.Message("message")
            .description("The message content to send");

    private final @NotNull SocialService social;

    public MsgCommand(@NotNull SessionManager sessions, @NotNull ChatMessageListener messages, @NotNull SocialService social) {
        super(messages, "msg");

        this.targetArg = CoreArgument.AnyOnlinePlayer("player", sessions)
                .description("The player to send the message to");

        this.description = "Send a direct message to a player";
        this.category = CommandCategories.SOCIAL;

        this.social = social;

        addSyntax(playerOnly(this::handleSendDirectMessage), targetArg, messageArg);
    }

    private void handleSendDirectMessage(@NotNull Player player, @NotNull CommandContext context) {
        var targetId = context.get(targetArg);
        var message = context.get(messageArg);

        if (targetId == null) {
            player.sendMessage(Component.translatable("generic.other_players_only"));
            return;
        }
        if (player.getUuid().toString().equals(targetId)) {
            player.sendMessage(Component.translatable("chat.msg.cant_message_yourself"));
            return;
        }

        if (SocialUtil.failIfBlocked(this.social, player, targetId, context.getRaw(this.targetArg), true)) return;

        this.handle(player, ChatChannel.DIRECT, targetId, message);
    }
}
