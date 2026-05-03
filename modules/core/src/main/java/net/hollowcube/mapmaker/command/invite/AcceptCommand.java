package net.hollowcube.mapmaker.command.invite;

import net.hollowcube.command.CommandContext;
import net.hollowcube.mapmaker.api.players.PlayerClient;
import net.hollowcube.mapmaker.invite.PlayerInviteService;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.session.SessionManager;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AcceptCommand extends AbstractInviteServiceCommand {

    public AcceptCommand(@NotNull PlayerInviteService inviteService, @NotNull PlayerService playerService,
                         @NotNull PlayerClient players, @NotNull SessionManager sessionManager) {
        super("accept", inviteService, playerService, players, sessionManager, "The player to accept the invite from", false);

        description = "Accept any pending request or invite from a player";

        this.addSyntax(playerOnly(this::handleDefaultAccept));
    }

    @Override
    void handle(@NotNull Player sender, @NotNull String targetId, @NotNull String targetName) {
        this.inviteService.accept(sender, targetId);
    }

    private void handleDefaultAccept(@NotNull Player player, @NotNull CommandContext context) {
        this.inviteService.accept(player);
    }
}
