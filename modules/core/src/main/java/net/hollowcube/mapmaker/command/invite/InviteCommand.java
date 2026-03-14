package net.hollowcube.mapmaker.command.invite;

import net.hollowcube.mapmaker.invite.PlayerInviteService;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.session.SessionManager;
import net.minestom.server.entity.Player;

public class InviteCommand extends AbstractInviteServiceCommand {

    public InviteCommand(
        PlayerInviteService inviteService, PlayerService playerService, SessionManager sessionManager
    ) {
        super("invite", inviteService, playerService, sessionManager, "The player to invite", true);

        description = "Sends an invite to a player for them to build or play with you";
    }

    @Override
    void handle(Player sender, String targetId, String targetName) {
        this.inviteService.registerInvite(sender, targetId);
    }
}
