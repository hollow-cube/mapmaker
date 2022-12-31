package net.hollowcube.mapmaker.dev;

import net.hollowcube.map.MapServerBase;
import net.hollowcube.mapmaker.bridge.MapToHubBridge;
import net.hollowcube.mapmaker.storage.MapStorage;
import net.hollowcube.mapmaker.storage.SaveStateStorage;
import net.hollowcube.world.WorldManager;
import org.jetbrains.annotations.NotNull;

public class DevMapServer extends MapServerBase {
    private final MapStorage mapStorage;
    private final SaveStateStorage saveStateStorage;

    private final WorldManager worldManager;

    public DevMapServer(
            @NotNull MapToHubBridge bridge,
            @NotNull MapStorage mapStorage,
            @NotNull SaveStateStorage saveStateStorage,
            @NotNull WorldManager worldManager
    ) {
        super(bridge);
        this.mapStorage = mapStorage;
        this.saveStateStorage = saveStateStorage;
        this.worldManager = worldManager;
    }

    @Override
    public @NotNull MapStorage mapStorage() {
        return mapStorage;
    }

    @Override
    public @NotNull SaveStateStorage saveStateStorage() {
        return saveStateStorage;
    }

    @Override
    public @NotNull WorldManager worldManager() {
        return worldManager;
    }

}
