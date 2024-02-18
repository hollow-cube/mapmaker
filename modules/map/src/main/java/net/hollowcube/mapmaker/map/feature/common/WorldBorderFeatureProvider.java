package net.hollowcube.mapmaker.map.feature.common;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.world.EditingMapWorld;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.MapSize;
import org.jetbrains.annotations.NotNull;

@AutoService(FeatureProvider.class)
public class WorldBorderFeatureProvider implements FeatureProvider {

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        var mapSize = world.map().settings().getSize();
        if (mapSize == null) {
            mapSize = MapSize.NORMAL;
        }

        var worldBorder = world.instance().getWorldBorder();
        worldBorder.setCenter(0f, 0f);
        worldBorder.setDiameter(mapSize.size());

        // Only add the red border if the map is an editing map
        if (world instanceof EditingMapWorld) {
            worldBorder.setWarningBlocks(5);
            worldBorder.setWarningTime(5);
        }

        return true;
    }

}
