package net.hollowcube.map.feature.experimental.marker;

import com.google.auto.service.AutoService;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.item.ItemHandler;
import net.hollowcube.map.world.MapWorld;
import org.jetbrains.annotations.NotNull;

@AutoService(FeatureProvider.class)
public class MarkerFeatureProvider implements FeatureProvider {

    private static final ItemHandler MARKER_ITEM = new MarkerItemHandler();

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        // Only enabled in editing worlds.
        if ((world.flags() & MapWorld.FLAG_EDITING) == 0 || true)
            return false;

        world.itemRegistry().register(MARKER_ITEM);

        return true;
    }

}
