package net.hollowcube.mapmaker.command.invite;

import net.hollowcube.ipc.player.PlayerService;
import net.hollowcube.ipc.player.SocialService;
import net.hollowcube.mapmaker.invite.PlayerInviteService;
import net.hollowcube.mapmaker.session.SessionManager;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RequestCommand extends AbstractInviteServiceCommand {

    public RequestCommand(@NotNull PlayerInviteService inviteService, @NotNull SocialService social,
                          @NotNull PlayerService players, @NotNull SessionManager sessionManager) {
        super("request", inviteService, social, players, sessionManager, "The player to request to join", true);

        description = "Sends a request to a player for you to build with them";
    }

    @Override
    void handle(@NotNull Player sender, @NotNull String targetId, @NotNull String targetName) {
        this.inviteService.registerRequest(sender, targetId);
    }
}
