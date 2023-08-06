package net.hollowcube.map.feature.mapsize;

import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.world.EditingMapWorld;
import net.hollowcube.map.world.MapWorld;
import org.jetbrains.annotations.NotNull;

public class MapSizeFeature implements FeatureProvider {

    private final MapSizeData mapSizeData;

    public MapSizeFeature(@NotNull MapSizeData mapSizeData) {
        this.mapSizeData = mapSizeData;
    }

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if (world instanceof EditingMapWorld editingMapWorld) {
            editingMapWorld.setMapSizeData(mapSizeData);
            return true;
        }
        return false;
    }
}
