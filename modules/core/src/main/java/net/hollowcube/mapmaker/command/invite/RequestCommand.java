package net.hollowcube.mapmaker.command.invite;

import net.hollowcube.mapmaker.invite.PlayerInviteService;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.session.SessionManager;
import net.minestom.server.entity.Player;

public class RequestCommand extends AbstractInviteServiceCommand {

    public RequestCommand(
        PlayerInviteService inviteService, PlayerService playerService, SessionManager sessionManager
    ) {
        super("request", inviteService, playerService, sessionManager, "The player to request to join", true);

        description = "Sends a request to a player for you to build with them";
    }

    @Override
    void handle(Player sender, String targetId, String targetName) {
        this.inviteService.registerRequest(sender, targetId);
    }
}
