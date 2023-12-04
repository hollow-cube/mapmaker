package net.hollowcube.map.feature;

import com.google.auto.service.AutoService;
import net.hollowcube.map.world.MapWorld;
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
        worldBorder.setWarningBlocks(5);
        worldBorder.setWarningTime(5);

        return true;
    }

}
