package net.hollowcube.mapmaker.dev;

import net.hollowcube.mapmaker.bridge.HubToMapBridge;
import net.hollowcube.mapmaker.hub.HubServerBase;
import net.hollowcube.mapmaker.invite.PlayerInviteService;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.player.SessionService;
import net.hollowcube.mapmaker.session.SessionManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Objects;

public class DevHubServer extends HubServerBase {
    private final HubToMapBridge bridge;

    private final PlayerService playerService;
    private final SessionService sessionService;
    private final MapService mapService;
    private final PermManager permManager;
    private final SessionManager sessionManager;
    private final PlayerInviteService pis;

    public DevHubServer(
            @NotNull HubToMapBridge bridge,
            @NotNull PlayerService playerService,
            @NotNull SessionService sessionService,
            @NotNull MapService mapService,
            @NotNull PermManager permManager,
            @NotNull SessionManager sessionManager,
            @NotNull PlayerInviteService pis
    ) {
        this.bridge = Objects.requireNonNull(bridge);
        this.playerService = Objects.requireNonNull(playerService);
        this.sessionService = Objects.requireNonNull(sessionService);
        this.mapService = Objects.requireNonNull(mapService);
        this.permManager = Objects.requireNonNull(permManager);
        this.sessionManager = Objects.requireNonNull(sessionManager);
        this.pis = Objects.requireNonNull(pis);
    }

    @Override
    public @NotNull HubToMapBridge bridge() {
        return this.bridge;
    }

    @Override
    public @NotNull PlayerService playerService() {
        return playerService;
    }

    @Override
    public @NotNull SessionService sessionService() {
        return sessionService;
    }

    @Override
    public @NotNull MapService mapService() {
        return mapService;
    }

    @Override
    public @NotNull PermManager permManager() {
        return permManager;
    }

    @Override
    public @NotNull PlayerInviteService inviteService() {
        return pis;
    }

    @Override
    public @UnknownNullability SessionManager sessionManager() {
        return sessionManager;
    }
}
