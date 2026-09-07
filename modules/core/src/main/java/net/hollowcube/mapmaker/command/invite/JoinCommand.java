package net.hollowcube.mapmaker.command.invite;

import net.hollowcube.ipc.player.PlayerService;
import net.hollowcube.ipc.player.SocialService;
import net.hollowcube.mapmaker.invite.PlayerInviteService;
import net.hollowcube.mapmaker.session.SessionManager;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class JoinCommand extends AbstractInviteServiceCommand {

    public JoinCommand(@NotNull PlayerInviteService inviteService, @NotNull SocialService social,
                       @NotNull PlayerService players, @NotNull SessionManager sessionManager) {
        super("join", inviteService, social, players, sessionManager, "The player to join", false);

        description = "Teleports you to the public map someone is playing, or if they are in a private world, prompts you to request to join them";
    }

    @Override
    void handle(@NotNull Player sender, @NotNull String targetId, @NotNull String targetName) {
        this.inviteService.join(sender, targetId);
    }
}
