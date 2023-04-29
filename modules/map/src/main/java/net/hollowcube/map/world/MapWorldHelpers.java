package net.hollowcube.map.world;

import jdk.incubator.concurrent.StructuredTaskScope;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.model.SaveState;
import net.hollowcube.mapmaker.storage.SaveStateStorage;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;

class MapWorldHelpers {
    private MapWorldHelpers() {
    }

    public static @NotNull List<FeatureProvider> loadFeatures(@NotNull InternalMapWorldNew world) {
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

    public static @Blocking @NotNull SaveState getOrCreateSaveState(
            @NotNull InternalMapWorldNew world,
            @NotNull String playerId,
            @NotNull SaveState.Type stateType
    ) {
        var saveStateStorage = world.server().saveStateStorage();
        var map = world.map();

        SaveState saveState;
        try {
            saveState = saveStateStorage.getLatestSaveState(playerId, map.getId(), stateType);
        } catch (SaveStateStorage.NotFoundError e) {
            saveState = new SaveState();
            saveState.setId(UUID.randomUUID().toString());
            saveState.setPlayerId(playerId);
            saveState.setMapId(map.getId());
            saveState.setStartTime(Instant.now());
            saveState.setPos(map.getSpawnPoint());

            saveState.setType(stateType);

            saveStateStorage.createSaveState(saveState);
        }
        return saveState;
    }

}
