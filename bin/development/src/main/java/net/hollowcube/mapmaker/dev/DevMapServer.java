package net.hollowcube.mapmaker.dev;

import net.hollowcube.map.MapServerBase;
import net.hollowcube.mapmaker.bridge.MapToHubBridge;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.player.SessionService;
import org.jetbrains.annotations.NotNull;

public class DevMapServer extends MapServerBase {
    private final PlayerService playerService;
    private final SessionService sessionService;
    private final MapService mapService;
    private final PermManager permManager;

    private final MapToHubBridge bridge;

    public DevMapServer(
            @NotNull MapToHubBridge bridge,
            @NotNull PlayerService playerService,
            @NotNull SessionService sessionService,
            @NotNull MapService mapService,
            @NotNull PermManager permManager
    ) {
        this.playerService = playerService;
        this.sessionService = sessionService;
        this.mapService = mapService;
        this.permManager = permManager;

        this.bridge = bridge;
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
    public @NotNull MapToHubBridge bridge() {
        return bridge;
    }
}
