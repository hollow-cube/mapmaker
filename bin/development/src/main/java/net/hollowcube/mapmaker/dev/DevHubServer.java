package net.hollowcube.mapmaker.dev;

import net.hollowcube.mapmaker.bridge.HubToMapBridge;
import net.hollowcube.mapmaker.hub.HubServerBase;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.player.SessionService;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class DevHubServer extends HubServerBase {
    private final PlayerService playerService;
    private final SessionService sessionService;
    private final MapService mapService;

    public DevHubServer(
            @NotNull HubToMapBridge bridge,
            @NotNull PlayerService playerService,
            @NotNull SessionService sessionService,
            @NotNull MapService mapService
    ) {
        super(bridge);
        this.playerService = Objects.requireNonNull(playerService);
        this.sessionService = Objects.requireNonNull(sessionService);
        this.mapService = Objects.requireNonNull(mapService);
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

}
