package net.hollowcube.mapmaker.map.feature.experimental;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.map.MapFeatureFlags;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.item.experimental.MarkerItem;
import net.hollowcube.mapmaker.map.world.EditingMapWorld;
import net.hollowcube.mapmaker.map.MapWorld;
import org.jetbrains.annotations.NotNull;

@AutoService(FeatureProvider.class)
public class MarkerFeatureProvider implements FeatureProvider {

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if (!MapFeatureFlags.MARKER_TOOL.test(world.map()))
            return false;

        // Only enabled in editing worlds.
        if (!(world instanceof EditingMapWorld))
            return false;

        world.itemRegistry().register(MarkerItem.INSTANCE);

        return true;
    }

}
