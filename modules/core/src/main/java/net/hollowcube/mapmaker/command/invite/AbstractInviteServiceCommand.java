package net.hollowcube.mapmaker.command.invite;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.invite.PlayerInviteService;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.session.SessionManager;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

abstract class AbstractInviteServiceCommand extends CommandDsl {

    protected final PlayerInviteService inviteService;
    private final PlayerService playerService;
    private final SessionManager sessionManager;
    private final Argument<String> targetArgument;
    private final boolean preventBlocked;

    AbstractInviteServiceCommand(@NotNull String command, @NotNull PlayerInviteService inviteService,
                                 @NotNull PlayerService playerService, @NotNull SessionManager sessionManager,
                                 @NotNull String playerArgDescription, boolean preventBlocked) {
        super(command);
        this.inviteService = inviteService;
        this.playerService = playerService;
        this.sessionManager = sessionManager;
        this.preventBlocked = preventBlocked;

        this.targetArgument = CoreArgument.AnyOnlinePlayer("player", sessionManager)
                .description(playerArgDescription);
        this.category = CommandCategories.SOCIAL;

        this.addSyntax(playerOnly(this::handleCommand), this.targetArgument);
    }

    abstract void handle(@NotNull Player sender, @NotNull String targetId, @NotNull String targetName);

    private void handleCommand(@NotNull Player player, @NotNull CommandContext context) {
        var targetName = context.getRaw(this.targetArgument);

        var targetId = context.get(this.targetArgument);
        if (targetId == null) {
            player.sendMessage(Component.translatable("generic.player.offline", Component.text(targetName)));
            return;
        }
        if (this.preventBlocked && this.playerService.failIfBlocked(player, targetId, targetName, true)) {
            return;
        }

        var targetSession = this.sessionManager.getSession(targetId);
        var targetDisplayName = this.playerService.getPlayerDisplayName2(targetId);
        if (targetSession == null) {
            player.sendMessage(Component.translatable("generic.player.offline", targetDisplayName));
            return;
        }
        if (player.getUuid().toString().equals(targetSession.playerId())) {
            player.sendMessage(Component.translatable("generic.other_players_only"));
            return;
        }

        this.handle(player, targetSession.playerId(), targetName);
    }
}
