package net.hollowcube.mapmaker.dev;

import net.hollowcube.map.MapServerBase;
import net.hollowcube.mapmaker.bridge.MapToHubBridge;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.permission.PlatformPermissionManager;
import net.hollowcube.mapmaker.storage.MetricStorage;
import org.jetbrains.annotations.NotNull;

public class DevMapServer extends MapServerBase {
    private final MapService mapService;

    private final MetricStorage metricStorage;

    private final PlatformPermissionManager platformPermissions;

    public DevMapServer(
            @NotNull MapToHubBridge bridge,
            @NotNull MapService mapService,
            @NotNull MetricStorage metricStorage,
            @NotNull PlatformPermissionManager platformPermissions
    ) {
        super(bridge);
        this.mapService = mapService;
        this.metricStorage = metricStorage;
        this.platformPermissions = platformPermissions;
    }

    @Override
    public @NotNull MetricStorage metricStorage() {
        return metricStorage;
    }

    @Override
    public @NotNull MapService mapService() {
        return mapService;
    }

    @Override
    public @NotNull PlatformPermissionManager platformPermissions() {
        return platformPermissions;
    }

}
