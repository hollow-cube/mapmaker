package net.hollowcube.mapmaker.dev;

import net.hollowcube.common.result.FutureResult;
import net.hollowcube.map.MapServer;
import net.hollowcube.mapmaker.bridge.HubToMapBridge;
import net.hollowcube.mapmaker.bridge.MapToHubBridge;
import net.hollowcube.mapmaker.hub.HubServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DevServerBridge implements HubToMapBridge, MapToHubBridge {
    private HubServer hub = null;
    private MapServer map = null;

    public void setHubServer(@NotNull HubServer hub) {
        this.hub = hub;
    }

    public void setMapServer(@NotNull MapServer map) {
        this.map = map;
    }

    //
    // HubToMapBridge implementation
    //


    //
    // MapToHubBridge implementation
    //

    @Override
    public @NotNull FutureResult<Void> sendPlayerToHub(@NotNull Player player) {
        return FutureResult.wrap(player.setInstance(hub.world().instance()));
    }
}
