package net.hollowcube.mapmaker.command.invite;

import com.google.inject.Inject;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.CommandCategory;
import net.hollowcube.mapmaker.invite.PlayerInviteService;
import net.hollowcube.mapmaker.session.SessionManager;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RequestCommand extends CommandDsl {
    private final Argument<String> targetArg = Argument.Word("player");

    private final PlayerInviteService inviteService;
    private final SessionManager sessionManager;

    @Inject
    public RequestCommand(@NotNull PlayerInviteService inviteService, @NotNull SessionManager sessionManager) {
        super("request");
        this.inviteService = inviteService;
        this.sessionManager = sessionManager;

        category = CommandCategory.SOCIAL;

        addSyntax(playerOnly(this::handleRequest));
        addSyntax(playerOnly(this::handleRequest), targetArg);
    }

    private void handleRequest(@NotNull Player player, @NotNull CommandContext context) {
        String targetName = context.get(targetArg);

        var targetSession = this.sessionManager.getSessionByName(targetName);
        if (targetSession == null) {
            player.sendMessage(Component.translatable("generic.player_offline", Component.text(targetName)));
            return;
        }
        if (player.getUuid().toString().equals(targetSession.playerId())) {
            player.sendMessage(Component.translatable("generic.other_players_only"));
            return;
        }

        this.inviteService.registerRequest(player, targetSession.playerId());
    }
}
