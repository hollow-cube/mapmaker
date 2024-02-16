package net.hollowcube.mapmaker.dev;

import net.hollowcube.map.runtime.ServerBridge;
import net.hollowcube.map2.AbstractMapWorld;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Future;

public class DevServerBridge implements ServerBridge {
    public static final Tag<Future<AbstractMapWorld>> TARGET_WORLD = Tag.Transient("mapmaker:map/target_world");

    @Override
    public void joinMap(@NotNull Player player, @NotNull String mapId, @NotNull JoinMapState joinMapState) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void joinHub(@NotNull Player player) {
        throw new UnsupportedOperationException("not implemented");
    }

//    @Override
//    public @Nullable String getCurrentMap(@NotNull Player player) {
//        var world = MapWorld.forPlayerOptional(player);
//        return world == null ? null : world.map().id();
//    }
//
//    //
//    // HubToMapBridge implementation
//    //
//
//    @Override
//    public @Blocking void joinMap(@NotNull Player player, @NotNull String mapId, @NotNull JoinMapState joinMapState) {
//        var playerData = PlayerDataV2.fromPlayer(player);
//        var map = mapServer.mapService().getMap(playerData.id(), mapId);
//        ((MapServerBase) mapServer).joinMap(player, map, joinMapState);
//    }
//
//
//    //
//    // MapToHubBridge implementation
//    //
//
//    @Override
//    public @Blocking void sendPlayerToHub(@NotNull Player player) {
//        var world = MapWorld.forPlayerOptional(player);
//        if (world instanceof InternalMapWorld internalWorld) {
//            internalWorld.removePlayer(player);
//        }
//
//        if (!player.isOnline()) return;
//        player.setInstance(hub.world().instance(), player.getPosition().withCoord(0.5, 4, 0.5)).join();
//    }
}
