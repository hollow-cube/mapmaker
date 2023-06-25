package net.hollowcube.map.world;

import jdk.incubator.concurrent.StructuredTaskScope;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.SaveState;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

class MapWorldHelpers {
    private MapWorldHelpers() {
    }

    public static @NotNull List<FeatureProvider> loadFeatures(@NotNull InternalMapWorld world) {
        var enabledFeatures = new ArrayList<FeatureProvider>();
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            // Load each feature in parallel
            var features = world.server().features();
            var enabledFutures = new Future[features.size()];
            for (int i = 0; i < features.size(); i++) {
                var feature = features.get(i);
                enabledFutures[i] = scope.fork(() -> feature.initMap(world));
            }

            scope.join();

            // Add each feature to the enabled list if it is enabled.
            for (int i = 0; i < features.size(); i++) {
                var feature = features.get(i);
                if ((boolean) enabledFutures[i].resultNow()) {
                    enabledFeatures.add(feature);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return enabledFeatures;
    }

    @Blocking
    public static @NotNull SaveState getOrCreateSaveState(
            @NotNull InternalMapWorld world,
            @NotNull String playerId
    ) {
        var mapService = world.server().mapService();
        var map = world.map();

        try {
            return mapService.getLatestSaveState(map.id(), playerId);
        } catch (MapService.NotFoundError ignored) {
            return mapService.createSaveState(map.id(), playerId);
        }
    }

}
