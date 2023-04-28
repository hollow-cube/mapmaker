package net.hollowcube.mapmaker.dev;

import net.hollowcube.common.result.FutureResult;
import net.hollowcube.map.MapServer;
import net.hollowcube.map.MapServerBase;
import net.hollowcube.mapmaker.bridge.HubToMapBridge;
import net.hollowcube.mapmaker.bridge.MapToHubBridge;
import net.hollowcube.mapmaker.hub.HubServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

public class DevServerBridge implements HubToMapBridge, MapToHubBridge {
    private HubServer hub = null;
    private MapServer mapServer = null;

    public void setHubServer(@NotNull HubServer hub) {
        this.hub = hub;
    }

    public void setMapServer(@NotNull MapServer map) {
        this.mapServer = map;
    }

    //
    // HubToMapBridge implementation
    //

    @Override
    public @Blocking void joinMap(@NotNull Player player, @NotNull String mapId, boolean edit) {
        var map = mapServer.mapStorage().getMapById(mapId);
        ((MapServerBase) mapServer).joinMap(player, map, edit);
    }


    //
    // MapToHubBridge implementation
    //

    @Override
    public @Blocking void sendPlayerToHub(@NotNull Player player) {
        player.setInstance(hub.world().instance()).join();
    }
}
