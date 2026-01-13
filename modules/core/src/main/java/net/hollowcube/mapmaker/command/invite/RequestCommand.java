package net.hollowcube.mapmaker.command.invite;

import net.hollowcube.mapmaker.invite.PlayerInviteService;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.session.SessionManager;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RequestCommand extends AbstractInviteServiceCommand {

    public RequestCommand(@NotNull PlayerInviteService inviteService, @NotNull PlayerService playerService,
                          @NotNull SessionManager sessionManager) {
        super("request", inviteService, playerService, sessionManager, "The player to request to join", true);

        description = "Sends a request to a player for you to build with them";
    }

    @Override
    void handle(@NotNull Player sender, @NotNull String targetId, @NotNull String targetName) {
        if (this.playerService.failIfBlocked(sender, targetId, targetName, true)) return;

        this.inviteService.registerRequest(sender, targetId);
    }
}
