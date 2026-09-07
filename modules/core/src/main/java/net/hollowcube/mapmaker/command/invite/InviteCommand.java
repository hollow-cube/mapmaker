package net.hollowcube.mapmaker.command.invite;

import net.hollowcube.ipc.player.PlayerService;
import net.hollowcube.ipc.player.SocialService;
import net.hollowcube.mapmaker.invite.PlayerInviteService;
import net.hollowcube.mapmaker.session.SessionManager;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class InviteCommand extends AbstractInviteServiceCommand {

    public InviteCommand(@NotNull PlayerInviteService inviteService, @NotNull SocialService social,
                         @NotNull PlayerService players, @NotNull SessionManager sessionManager) {
        super("invite", inviteService, social, players, sessionManager, "The player to invite", true);

        description = "Sends an invite to a player for them to build or play with you";
    }

    @Override
    void handle(@NotNull Player sender, @NotNull String targetId, @NotNull String targetName) {
        this.inviteService.registerInvite(sender, targetId);
    }
}
