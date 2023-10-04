package net.hollowcube.map.feature.play;

import com.google.auto.service.AutoService;
import net.hollowcube.map.MapHooks;
import net.hollowcube.map.event.MapPlayerInitEvent;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.gui.hotbar.BuildingMapHotbar;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.map.MapVariant;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;

@AutoService(FeatureProvider.class)
public class BaseBuildMapFeatureProvide implements FeatureProvider {

    private final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:play/building", EventFilter.INSTANCE)
            .addListener(MapPlayerInitEvent.class, this::initPlayer);

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if ((world.flags() & MapWorld.FLAG_PLAYING) == 0)
            return false;
        if (world.map().settings().getVariant() != MapVariant.BUILDING)
            return false;

        world.addScopedEventNode(eventNode);

        return true;
    }

    public void initPlayer(@NotNull MapPlayerInitEvent event) {
        var player = event.getPlayer();
        if (!MapHooks.isPlayerPlaying(player)) return;

        player.setAllowFlying(true);

        if (event.isFirstInit()) {
            BuildingMapHotbar.applyToPlayer(event.mapWorld(), player);
        }
    }

}
