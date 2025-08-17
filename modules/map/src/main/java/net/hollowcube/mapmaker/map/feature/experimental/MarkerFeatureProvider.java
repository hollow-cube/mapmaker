package net.hollowcube.mapmaker.map.feature.experimental;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import org.jetbrains.annotations.NotNull;

@AutoService(FeatureProvider.class)
public class MarkerFeatureProvider implements FeatureProvider {

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        return false;
//        if (!MapFeatureFlags.MARKER_TOOL.test(world.map()))
//            return false;
//
//        // Only enabled in editing worlds.
//        if (!(world instanceof EditingMapWorld))
//            return false;
//
//        world.itemRegistry().register(MarkerItem.INSTANCE);
//
//        return true;
    }

}
