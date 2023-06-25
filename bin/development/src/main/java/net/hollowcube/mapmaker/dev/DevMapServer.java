package net.hollowcube.mapmaker.dev;

import net.hollowcube.map.MapServerBase;
import net.hollowcube.mapmaker.bridge.MapToHubBridge;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.player.SessionService;
import org.jetbrains.annotations.NotNull;

public class DevMapServer extends MapServerBase {
    private final PlayerService playerService;
    private final SessionService sessionService;
    private final MapService mapService;

    public DevMapServer(
            @NotNull MapToHubBridge bridge,
            @NotNull PlayerService playerService,
            @NotNull SessionService sessionService,
            @NotNull MapService mapService
    ) {
        super(bridge);
        this.playerService = playerService;
        this.sessionService = sessionService;
        this.mapService = mapService;
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
