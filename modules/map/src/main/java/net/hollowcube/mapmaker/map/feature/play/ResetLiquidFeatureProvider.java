package net.hollowcube.mapmaker.map.feature.play;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.event.vnext.MapPlayerResetEvent;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.util.PlayerLiquidExtension;
import net.hollowcube.mapmaker.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.map.world.TestingMapWorld;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;

@AutoService(FeatureProvider.class)
public class ResetLiquidFeatureProvider implements FeatureProvider {

    private final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:vanilla/trident", EventFilter.INSTANCE)
            .addListener(PlayerTickEvent.class, this::handlePlayerTick);

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if (!(world instanceof PlayingMapWorld || world instanceof TestingMapWorld))
            return false;
        if (!(world.map().getSetting(MapSettings.RESET_IN_WATER) || world.map().getSetting(MapSettings.RESET_IN_LAVA)))
            return false;

        world.eventNode().addChild(eventNode);
        return true;
    }

    private void handlePlayerTick(@NotNull PlayerTickEvent event) {
        var player = event.getPlayer();
        if (!(player instanceof PlayerLiquidExtension ple) || !(ple.isInWater() || ple.isInLava()))
            return;

        var world = MapWorld.forPlayerOptional(player);
        if (world == null || !world.isPlaying(player)) return; // Sanity

        boolean isWaterReset = world.map().getSetting(MapSettings.RESET_IN_WATER) && ple.isInWater();
        boolean isLavaReset = isWaterReset || world.map().getSetting(MapSettings.RESET_IN_LAVA) && ple.isInLava();
        if (isWaterReset || isLavaReset) {
            world.callEvent(new MapPlayerResetEvent(player, world, true));
        }
    }
}
