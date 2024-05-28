package net.hollowcube.mapmaker.map.feature.common;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.map.MapSize;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.world.EditingMapWorld;
import net.minestom.server.ServerFlag;
import net.minestom.server.instance.WorldBorder;
import org.jetbrains.annotations.NotNull;

@AutoService(FeatureProvider.class)
public class WorldBorderFeatureProvider implements FeatureProvider {

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        var mapSize = world.map().settings().getSize();
        if (mapSize == null) {
            mapSize = MapSize.NORMAL;
        }

        // Only add the red border if the map is an editing map
        int warning = world instanceof EditingMapWorld ? 5 : 0;

        var worldBorder = new WorldBorder(mapSize.size(), 0f, 0f, warning, warning, ServerFlag.WORLD_BORDER_SIZE);
        world.instance().setWorldBorder(worldBorder);

        return true;
    }

}
