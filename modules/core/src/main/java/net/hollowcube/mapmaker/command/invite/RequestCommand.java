package net.hollowcube.mapmaker.command.invite;

import com.google.inject.Inject;
import net.hollowcube.mapmaker.invite.PlayerInviteService;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.session.SessionManager;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RequestCommand extends AbstractInviteServiceCommand {

    @Inject
    public RequestCommand(@NotNull PlayerInviteService inviteService, @NotNull PlayerService playerService,
                          @NotNull SessionManager sessionManager) {
        super("request", inviteService, playerService, sessionManager);
    }

    @Override
    void handle(@NotNull Player sender, @NotNull String targetId) {
        this.inviteService.registerRequest(sender, targetId);
    }
}
