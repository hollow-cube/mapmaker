package net.hollowcube.mapmaker.command.invite;

import net.hollowcube.command.CommandContext;
import net.hollowcube.mapmaker.invite.PlayerInviteService;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.session.SessionManager;
import net.minestom.server.entity.Player;

public class AcceptCommand extends AbstractInviteServiceCommand {

    public AcceptCommand(
        PlayerInviteService inviteService, PlayerService playerService, SessionManager sessionManager
    ) {
        super("accept", inviteService, playerService, sessionManager, "The player to accept the invite from", false);

        description = "Accept any pending request or invite from a player";

        this.addSyntax(playerOnly(this::handleDefaultAccept));
    }

    @Override
    void handle(Player sender, String targetId, String targetName) {
        this.inviteService.accept(sender, targetId);
    }

    private void handleDefaultAccept(Player player, CommandContext context) {
        this.inviteService.accept(player);
    }
}
