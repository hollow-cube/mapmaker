package net.hollowcube.mapmaker.dev;

import net.hollowcube.mapmaker.bridge.HubToMapBridge;
import net.hollowcube.mapmaker.hub.HubServerBase;
import net.hollowcube.mapmaker.permission.MapPermissionManager;
import net.hollowcube.mapmaker.storage.MapStorage;
import net.hollowcube.mapmaker.storage.PlayerStorage;
import net.hollowcube.world.WorldManager;
import org.jetbrains.annotations.NotNull;

public class DevHubServer extends HubServerBase {
    private final PlayerStorage playerStorage;
    private final MapStorage mapStorage;
    private final WorldManager worldManager;

    private final MapPermissionManager mapPermissions;

    public DevHubServer(
            @NotNull HubToMapBridge bridge,
            @NotNull MapStorage mapStorage,
            @NotNull PlayerStorage playerStorage,
            @NotNull WorldManager worldManager,
            @NotNull MapPermissionManager mapPermissions) {
        super(bridge);
        this.playerStorage = playerStorage;
        this.mapStorage = mapStorage;
        this.worldManager = worldManager;
        this.mapPermissions = mapPermissions;
    }

    @Override
    public @NotNull PlayerStorage playerStorage() {
        return playerStorage;
    }

    @Override
    public @NotNull MapStorage mapStorage() {
        return mapStorage;
    }

    @Override
    public @NotNull WorldManager worldManager() {
        return worldManager;
    }

    @Override
    public @NotNull MapPermissionManager mapPermissions() {
        return mapPermissions;
    }
}
