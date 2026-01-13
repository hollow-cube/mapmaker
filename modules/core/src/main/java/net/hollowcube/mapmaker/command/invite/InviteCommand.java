package net.hollowcube.mapmaker.command.invite;

import net.hollowcube.mapmaker.invite.PlayerInviteService;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.session.SessionManager;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class InviteCommand extends AbstractInviteServiceCommand {

    public InviteCommand(@NotNull PlayerInviteService inviteService, @NotNull PlayerService playerService,
                         @NotNull SessionManager sessionManager) {
        super("invite", inviteService, playerService, sessionManager, "The player to invite", true);

        description = "Sends an invite to a player for them to build or play with you";
    }

    @Override
    void handle(@NotNull Player sender, @NotNull String targetId, @NotNull String targetName) {
        if (this.playerService.failIfBlocked(sender, targetId, targetName, true)) return;

        this.inviteService.registerInvite(sender, targetId);
    }
}
