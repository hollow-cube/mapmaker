package net.hollowcube.mapmaker.dev;

import net.hollowcube.mapmaker.bridge.HubToMapBridge;
import net.hollowcube.mapmaker.hub.HubServerBase;
import net.hollowcube.mapmaker.hub.find_a_new_home.legacy.LegacyMapService;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.service.PlayerService;
import net.hollowcube.mapmaker.storage.MetricStorage;
import net.hollowcube.mapmaker.storage.PlayerStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class DevHubServer extends HubServerBase {
    private final MapService mapService;

    private final PlayerStorage playerStorage;
    private final MetricStorage metricStorage;

    private final @NotNull PlayerService playerService;

    private final LegacyMapService legacyMapService;

    public DevHubServer(
            @NotNull HubToMapBridge bridge,
            @NotNull MapService mapService,
            @NotNull PlayerStorage playerStorage,
            @NotNull MetricStorage metricStorage,
            @NotNull PlayerService playerService,
            @Nullable LegacyMapService legacyMapService) {
        super(bridge);
        this.playerStorage = Objects.requireNonNull(playerStorage);
        this.mapService = Objects.requireNonNull(mapService);
        this.metricStorage = Objects.requireNonNull(metricStorage);
        this.playerService = Objects.requireNonNull(playerService);
        this.legacyMapService = legacyMapService;
    }

    @Override
    public @NotNull PlayerStorage playerStorage() {
        return playerStorage;
    }

    @Override
    public @NotNull MapService mapService() {
        return mapService;
    }

    @Override
    public @NotNull MetricStorage metricStorage() {
        return metricStorage;
    }

    @Override
    public @NotNull PlayerService playerService() {
        return playerService;
    }

    @Override
    public @Nullable LegacyMapService legacyMapService() {
        return legacyMapService;
    }
}
