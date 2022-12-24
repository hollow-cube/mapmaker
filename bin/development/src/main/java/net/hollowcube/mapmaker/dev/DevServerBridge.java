package net.hollowcube.mapmaker.dev;

import net.hollowcube.common.result.FutureResult;
import net.hollowcube.map.MapServer;
import net.hollowcube.map.MapServerBase;
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

    @Override
    public @NotNull FutureResult<Void> joinMap(@NotNull Player player, @NotNull String mapId, boolean edit) {
        return map.mapStorage().getMapById(mapId)
                .flatMap(map -> ((MapServerBase) this.map).joinMap(player, map, edit));
    }


    //
    // MapToHubBridge implementation
    //

    @Override
    public @NotNull FutureResult<Void> sendPlayerToHub(@NotNull Player player) {
        return FutureResult.wrap(player.setInstance(hub.world().instance()));
    }
}
