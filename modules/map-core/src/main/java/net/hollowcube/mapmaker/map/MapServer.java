package net.hollowcube.mapmaker.map;

import net.hollowcube.command.CommandManager;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.invite.PlayerInviteService;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.player.SessionService;
import net.hollowcube.mapmaker.punishments.PunishmentService;
import net.hollowcube.mapmaker.session.SessionManager;
import net.hollowcube.mapmaker.util.ServiceContext;
import net.minestom.server.timer.Scheduler;
import org.jetbrains.annotations.NotNull;

public interface MapServer {

    @NotNull ApiClient api();

    @NotNull SessionService sessionService();

    @NotNull PlayerService playerService();

    @NotNull PunishmentService punishmentService();

    @NotNull PlayerInviteService inviteService();

    default @NotNull ServiceContext createServiceContext() {
        return new ServiceContext(
            this.api(),
            this.playerService(),
            this.bridge()
        );
    }

    // Higher level managers

    @NotNull SessionManager sessionManager();

    @NotNull ServerBridge bridge();

    @NotNull CommandManager commandManager();

    @NotNull Scheduler scheduler();

}
