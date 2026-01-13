package net.hollowcube.mapmaker.command.invite;

import net.hollowcube.command.CommandContext;
import net.hollowcube.mapmaker.invite.PlayerInviteService;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.session.SessionManager;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RejectCommand extends AbstractInviteServiceCommand {

    public RejectCommand(@NotNull PlayerInviteService inviteService, @NotNull PlayerService playerService,
                         @NotNull SessionManager sessionManager) {
        super("reject", inviteService, playerService, sessionManager, "The player to reject", false);

        description = "Denies any pending request or invite from a player";

        this.addSyntax(playerOnly(this::handleDefaultReject));
    }

    @Override
    void handle(@NotNull Player sender, @NotNull String targetId, @NotNull String targetName) {
        this.inviteService.reject(sender, targetId);
    }

    private void handleDefaultReject(@NotNull Player player, @NotNull CommandContext context) {
        this.inviteService.reject(player);
    }
}
