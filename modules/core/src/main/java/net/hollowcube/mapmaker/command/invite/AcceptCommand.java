package net.hollowcube.mapmaker.command.invite;

import com.google.inject.Inject;
import net.hollowcube.command.CommandContext;
import net.hollowcube.mapmaker.invite.PlayerInviteService;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.session.SessionManager;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AcceptCommand extends AbstractInviteServiceCommand {

    @Inject
    public AcceptCommand(@NotNull PlayerInviteService inviteService, @NotNull PlayerService playerService,
                         @NotNull SessionManager sessionManager) {
        super("accept", inviteService, playerService, sessionManager);

        this.addSyntax(playerOnly(this::handleDefaultAccept));
    }

    @Override
    void handle(@NotNull Player sender, @NotNull String targetId) {
        this.inviteService.accept(sender, targetId);
    }

    private void handleDefaultAccept(@NotNull Player player, @NotNull CommandContext context) {
        this.inviteService.accept(player);
    }
}
