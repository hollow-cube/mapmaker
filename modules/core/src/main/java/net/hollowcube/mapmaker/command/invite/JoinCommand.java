package net.hollowcube.mapmaker.command.invite;

import net.hollowcube.mapmaker.invite.PlayerInviteService;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.session.SessionManager;
import net.minestom.server.entity.Player;

public class JoinCommand extends AbstractInviteServiceCommand {

    public JoinCommand(
        PlayerInviteService inviteService, PlayerService playerService, SessionManager sessionManager
    ) {
        super("join", inviteService, playerService, sessionManager, "The player to join", false);

        description = "Teleports you to the public map someone is playing, or if they are in a private world, prompts you to request to join them";
    }

    @Override
    void handle(Player sender, String targetId, String targetName) {
        this.inviteService.join(sender, targetId);
    }
}
