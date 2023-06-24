package net.hollowcube.mapmaker.dev;

import net.hollowcube.map.MapServerBase;
import net.hollowcube.mapmaker.bridge.MapToHubBridge;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.permission.PlatformPermissionManager;
import net.hollowcube.mapmaker.storage.MetricStorage;
import net.hollowcube.mapmaker.storage.SaveStateStorage;
import org.jetbrains.annotations.NotNull;

public class DevMapServer extends MapServerBase {
    private final MapService mapService;

    private final MetricStorage metricStorage;
    private final SaveStateStorage saveStateStorage;

    private final PlatformPermissionManager platformPermissions;

    public DevMapServer(
            @NotNull MapToHubBridge bridge,
            @NotNull MapService mapService,
            @NotNull MetricStorage metricStorage,
            @NotNull SaveStateStorage saveStateStorage,
            @NotNull PlatformPermissionManager platformPermissions
    ) {
        super(bridge);
        this.mapService = mapService;
        this.metricStorage = metricStorage;
        this.saveStateStorage = saveStateStorage;
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
    public @NotNull SaveStateStorage saveStateStorage() {
        return saveStateStorage;
    }

    @Override
    public @NotNull PlatformPermissionManager platformPermissions() {
        return platformPermissions;
    }

}
