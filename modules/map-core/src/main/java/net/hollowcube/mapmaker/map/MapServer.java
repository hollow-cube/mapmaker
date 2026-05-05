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

    @NotNull MapService mapService();

    @NotNull PunishmentService punishmentService();

    @NotNull PlayerInviteService inviteService();

    default @NotNull ServiceContext createServiceContext() {
        return new ServiceContext(
            this.api(),
            this.playerService(),
            this.sessionService(),
            this.mapService(),
            this.bridge()
        );
    }

    // Higher level managers

    @NotNull SessionManager sessionManager();

    @NotNull ServerBridge bridge();

    @NotNull CommandManager commandManager();

    @NotNull Scheduler scheduler();

    /**
     * More generic API to get facets of the server which may or may not be present in different environments.
     *
     * @param type The type of the facet
     * @param <T>  facet type
     * @return The facet
     * @throws IllegalArgumentException if the facet is not present
     */
    <T> @NotNull T facet(@NotNull Class<T> type);

}
