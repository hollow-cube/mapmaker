package net.hollowcube.mapmaker.command.chat;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.mapmaker.chat.ChatMessageListener;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.player.BlockedPlayer;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.session.SessionManager;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MsgCommand extends AbstractChatCommand {

    private final Argument<String> targetArg;
    private final Argument<String> messageArg = CoreArgument.Message("message")
            .description("The message content to send");

    private final @NotNull PlayerService playerService;

    public MsgCommand(@NotNull SessionManager sessions, @NotNull MapService maps, @NotNull ChatMessageListener messages, @NotNull PlayerService playerService) {
        super(sessions, maps, messages, "msg");

        this.targetArg = CoreArgument.AnyOnlinePlayer("player", sessions)
                .description("The player to send the message to");

        this.description = "Send a direct message to a player";
        this.category = CommandCategories.SOCIAL;

        this.playerService = playerService;

        addSyntax(playerOnly(this::handleSendDirectMessage), targetArg, messageArg);
    }

    private void handleSendDirectMessage(@NotNull Player player, @NotNull CommandContext context) {
        var targetId = context.get(targetArg);
        var message = context.get(messageArg);

        if (targetId == null) {
            player.sendMessage(Component.translatable("generic.other_players_only"));
            return;
        }
        if (PlayerData.fromPlayer(player).id().equals(targetId)) {
            player.sendMessage(Component.translatable("chat.msg.cant_message_yourself"));
            return;
        }

        var blocks = this.playerService.getBlocksBetween(targetId, player.getUuid().toString(), true);
        if (!blocks.isEmpty()) {
            BlockedPlayer block = blocks.getFirst();
            if (block.playerId().equals(player.getUuid().toString())) { // blocked by target
                player.sendMessage(Component.translatable("chat.msg.blocked_by_target", Component.text(context.getRaw(this.targetArg))));
            } else { // blocked by self
                player.sendMessage(Component.translatable("chat.msg.blocked_by_self", Component.text(context.getRaw(this.targetArg))));
            }
            return;
        }

        this.handle(player, targetId, message);
    }
}
