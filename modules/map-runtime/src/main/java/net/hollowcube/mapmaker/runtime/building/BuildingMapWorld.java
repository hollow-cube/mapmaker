package net.hollowcube.mapmaker.runtime.building;

import net.hollowcube.mapmaker.map.AbstractMapWorld;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapServer;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.util.EventUtil;
import net.hollowcube.mapmaker.misc.BossBars;
import net.hollowcube.mapmaker.runtime.item.MapDetailsItem;
import net.hollowcube.mapmaker.runtime.parkour.item.RateMapItem;
import net.hollowcube.mapmaker.runtime.parkour.item.ReturnToHubItem;
import net.kyori.adventure.bossbar.BossBar;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BuildingMapWorld extends AbstractMapWorld<BuildingState, BuildingMapWorld> {

    public static @Nullable BuildingMapWorld forPlayer(Player player) {
        return MapWorld.forPlayer(BuildingMapWorld.class, player);
    }

    public BuildingMapWorld(MapServer server, MapData map) {
        super(server, map, makeMapInstance(map, 'b'), BuildingState.class);

        itemRegistry().registerSilent(MapDetailsItem.INSTANCE);
        itemRegistry().registerSilent(ReturnToHubItem.INSTANCE);
        itemRegistry().registerSilent(RateMapItem.INSTANCE);

        eventNode()
                .addListener(PlayerTickEvent.class, this::handlePlayerTick)
                .addChild(EventUtil.READ_ONLY_NODE);
    }

    @Override
    protected BuildingState configurePlayer(Player player) {
        if (RateMapItem.isMapRatable(this)) {
            RateMapItem.initLastRating(server().api().maps, player, map());
        }

        player.setRespawnPoint(map().settings().getSpawnPoint());

        return new BuildingState.Building();
    }

    private void handlePlayerTick(PlayerTickEvent event) {
        final var player = event.getPlayer();

        var minHeight = instance().getCachedDimensionType().minY() - 20;
        if (player.getPosition().y() < minHeight)
            player.teleport(map().settings().getSpawnPoint());
    }

    @Override
    protected @Nullable List<BossBar> createBossBars() {
        return BossBars.createPlayingBossBar(server().api().players, map());
    }
}
