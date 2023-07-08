package net.hollowcube.mapmaker.dev;

import net.hollowcube.map.MapServer;
import net.hollowcube.map.MapServerBase;
import net.hollowcube.map.world.InternalMapWorld;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.bridge.HubToMapBridge;
import net.hollowcube.mapmaker.bridge.MapToHubBridge;
import net.hollowcube.mapmaker.hub.HubServer;
import net.hollowcube.mapmaker.player.PlayerDataV2;
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
        var playerData = PlayerDataV2.fromPlayer(player);
        var map = mapServer.mapService().getMap(playerData.id(), mapId);
        ((MapServerBase) mapServer).joinMap(player, map, edit);
    }


    //
    // MapToHubBridge implementation
    //

    @Override
    public @Blocking void sendPlayerToHub(@NotNull Player player) {

        var world = MapWorld.forPlayerOptional(player);
        if (world instanceof InternalMapWorld internalWorld) {
            internalWorld.removePlayer(player);
        }
        player.setInstance(hub.world().instance(), player.getPosition().withCoord(0.5, 4, 0.5)).join();
    }
}
