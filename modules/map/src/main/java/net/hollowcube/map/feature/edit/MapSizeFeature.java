package net.hollowcube.map.feature.edit;

import com.google.auto.service.AutoService;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.map.MapSize;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;

@AutoService(FeatureProvider.class)
public class MapSizeFeature implements FeatureProvider {

    private MapSize mapSize;

    public MapSizeFeature() {
    }

    private final EventNode<InstanceEvent> mapBoundaryNode = EventNode.type("mapmaker:feature/map-boundary", EventFilter.INSTANCE)
            .addListener(PlayerBlockPlaceEvent.class, this::onBlockPlace);

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if ((world.flags() & MapWorld.FLAG_EDITING) != 0) {
            mapSize = world.map().settings().getSize();
            if (mapSize == null) {
                mapSize = MapSize.NORMAL;
            }
            world.addScopedEventNode(mapBoundaryNode);
            world.instance().getWorldBorder().setCenter(0f, 0f);
            world.instance().getWorldBorder().setDiameter(mapSize.size());
            return true;
        }
        return false;
    }

    @Override
    public void cleanupMap(@NotNull MapWorld world) {
    }

    private void onBlockPlace(PlayerBlockPlaceEvent event) {
        var block = event.getBlockPosition();
        if (Math.abs(block.x()) > mapSize.size() || Math.abs(block.z()) > mapSize.size())
            event.setCancelled(true);
    }
}
