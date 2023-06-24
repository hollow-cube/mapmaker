package net.hollowcube.mapmaker.dev;

import net.hollowcube.map.MapServerBase;
import net.hollowcube.mapmaker.bridge.MapToHubBridge;
import net.hollowcube.mapmaker.map.MapService;
import org.jetbrains.annotations.NotNull;

public class DevMapServer extends MapServerBase {
    private final MapService mapService;

    public DevMapServer(
            @NotNull MapToHubBridge bridge,
            @NotNull MapService mapService
    ) {
        super(bridge);
        this.mapService = mapService;
    }

    @Override
    public @NotNull MapService mapService() {
        return mapService;
    }

}
