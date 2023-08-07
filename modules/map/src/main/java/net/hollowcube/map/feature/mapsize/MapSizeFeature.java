package net.hollowcube.map.feature.mapsize;

import com.google.auto.service.AutoService;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.world.MapWorld;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;

import static java.lang.Math.abs;

public class MapSizeFeature implements FeatureProvider {
    private final MapSizeData mapSizeData;

    public MapSizeFeature(MapSizeData mapSizeData) {
        this.mapSizeData = mapSizeData;
    }
    
    private final EventNode<InstanceEvent> mapBoundaryNode = EventNode.type("mapmaker:feature/map-boundary", EventFilter.INSTANCE)
            .addListener(PlayerBlockPlaceEvent.class, this::onBlockPlace);

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if ((world.flags() & MapWorld.FLAG_EDITING) != 0) {
            world.addScopedEventNode(mapBoundaryNode);
            world.instance().getWorldBorder().setCenter(0f, 0f);
            world.instance().getWorldBorder().setDiameter(mapSizeData.xLimit * mapSizeData.zLimit);
            return true;
        }
        return false;
    }

    @Override
    public void cleanupMap(@NotNull MapWorld world) {
        world.removeScopedEventNode(mapBoundaryNode);
    }

    private void onBlockPlace(PlayerBlockPlaceEvent event) {
        var block = event.getBlockPosition();
        if (abs(block.x()) > mapSizeData.xLimit ||
            abs(block.y()) > mapSizeData.yLimit ||
            abs(block.z()) > mapSizeData.zLimit)
                event.setCancelled(true);

    }
}
