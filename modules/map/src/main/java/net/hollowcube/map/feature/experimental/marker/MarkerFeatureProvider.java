package net.hollowcube.map.feature.experimental.marker;

import com.google.auto.service.AutoService;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.item.ItemHandler;
import net.hollowcube.map.world.EditingMapWorld;
import net.hollowcube.map.world.MapWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@AutoService(FeatureProvider.class)
public class MarkerFeatureProvider implements FeatureProvider {

    private static final ItemHandler MARKER_ITEM = new MarkerItemHandler();

    @Override
    public @Nullable ListenableFuture<Void> initMap(@NotNull MapWorld world) {
        // Only enabled in editing worlds.
        if (!(world instanceof EditingMapWorld))
            return null;

        world.itemRegistry().register(MARKER_ITEM);

        return Futures.immediateVoidFuture();
    }

}
