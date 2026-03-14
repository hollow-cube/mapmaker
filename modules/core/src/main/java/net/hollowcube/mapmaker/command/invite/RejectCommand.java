package net.hollowcube.mapmaker.command.invite;

import net.hollowcube.command.CommandContext;
import net.hollowcube.mapmaker.invite.PlayerInviteService;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.session.SessionManager;
import net.minestom.server.entity.Player;

public class RejectCommand extends AbstractInviteServiceCommand {

    public RejectCommand(
        PlayerInviteService inviteService, PlayerService playerService, SessionManager sessionManager
    ) {
        super("reject", inviteService, playerService, sessionManager, "The player to reject", false);

        description = "Denies any pending request or invite from a player";

        this.addSyntax(playerOnly(this::handleDefaultReject));
    }

    @Override
    void handle(Player sender, String targetId, String targetName) {
        this.inviteService.reject(sender, targetId);
    }

    private void handleDefaultReject(Player player, CommandContext context) {
        this.inviteService.reject(player);
    }
}
