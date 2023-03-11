package net.hollowcube.mapmaker.dev;

import net.hollowcube.mapmaker.bridge.HubToMapBridge;
import net.hollowcube.mapmaker.hub.HubServerBase;
import net.hollowcube.mapmaker.permission.MapPermissionManager;
import net.hollowcube.mapmaker.permission.PlatformPermissionManager;
import net.hollowcube.mapmaker.storage.MapStorage;
import net.hollowcube.mapmaker.storage.PlayerStorage;
import net.hollowcube.world.WorldManager;
import org.jetbrains.annotations.NotNull;

public class DevHubServer extends HubServerBase {
    private final PlayerStorage playerStorage;
    private final MapStorage mapStorage;
    private final WorldManager worldManager;

    private final PlatformPermissionManager platformPermissions;
    private final MapPermissionManager mapPermissions;

    public DevHubServer(
            @NotNull HubToMapBridge bridge,
            @NotNull MapStorage mapStorage,
            @NotNull PlayerStorage playerStorage,
            @NotNull WorldManager worldManager,
            @NotNull PlatformPermissionManager platformPermissions,
            @NotNull MapPermissionManager mapPermissions) {
        super(bridge);
        this.playerStorage = playerStorage;
        this.mapStorage = mapStorage;
        this.worldManager = worldManager;
        this.platformPermissions = platformPermissions;
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
    public @NotNull PlatformPermissionManager platformPermissions() {
        return platformPermissions;
    }

    @Override
    public @NotNull MapPermissionManager mapPermissions() {
        return mapPermissions;
    }
}
