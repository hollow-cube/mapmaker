package net.hollowcube.mapmaker.command.invite;

import com.google.inject.Inject;
import net.hollowcube.mapmaker.invite.PlayerInviteService;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.session.SessionManager;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class InviteCommand extends AbstractInviteServiceCommand {

    @Inject
    public InviteCommand(@NotNull PlayerInviteService inviteService, @NotNull PlayerService playerService,
                         @NotNull SessionManager sessionManager) {
        super("invite", inviteService, playerService, sessionManager);
    }

    @Override
    void handle(@NotNull Player sender, @NotNull String targetId) {
        this.inviteService.registerInvite(sender, targetId);
    }
}
