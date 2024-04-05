package net.hollowcube.mapmaker.dev;

import net.hollowcube.mapmaker.map.AbstractMapWorld;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.runtime.MapAllocator;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.map.world.EditingMapWorld;
import net.hollowcube.mapmaker.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Future;

public class DevServerBridge implements ServerBridge {
    public static final Tag<Future<? extends AbstractMapWorld>> TARGET_WORLD = Tag.Transient("mapmaker:map/target_world");

    private final MapService mapService;
    private final MapAllocator allocator;

    public DevServerBridge(@NotNull MapService mapService, @NotNull MapAllocator allocator) {
        this.mapService = mapService;
        this.allocator = allocator;
    }

    @Override
    public void joinMap(@NotNull Player player, @NotNull String mapId, @NotNull JoinMapState joinMapState, @NotNull String source) {
//        if (CoreFeatureFlags.MAP_DISABLE_ALL.test()) {
//            player.sendMessage(Component.translatable("ff.maps_disabled"));
//            return;
//        }

        var playerId = PlayerDataV2.fromPlayer(player).id();
        var map = mapService.getMap(playerId, mapId);
        Class<? extends AbstractMapWorld> worldType = switch (joinMapState) {
            case PLAYING, SPECTATING -> PlayingMapWorld.class;
            case EDITING -> EditingMapWorld.class;
        };
        var worldFuture = allocator.create(map, worldType);

        player.setTag(TARGET_WORLD, worldFuture);
        player.startConfigurationPhase();
    }

    @Override
    public void joinHub(@NotNull Player player) {
        // Any player reconfiguring without that tag will be sent to the hub, so simply remove it and reconfigure.
        player.removeTag(TARGET_WORLD);
        player.startConfigurationPhase();
    }

}
