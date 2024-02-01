package net.hollowcube.map.feature.experimental;

import com.google.auto.service.AutoService;
import net.hollowcube.map.MapFeatureFlags;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.item.experimental.MarkerItem;
import net.hollowcube.map.world.MapWorld;
import org.jetbrains.annotations.NotNull;

@AutoService(FeatureProvider.class)
public class MarkerFeatureProvider implements FeatureProvider {

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if (!MapFeatureFlags.MARKER_TOOL.test(world.map()))
            return false;

        // Only enabled in editing worlds.
        if ((world.flags() & MapWorld.FLAG_EDITING) == 0)
            return false;

        world.itemRegistry().register(MarkerItem.INSTANCE);

        return true;
    }

}
