package net.hollowcube.mapmaker.storage;

import net.hollowcube.common.config.MongoConfig;
import net.hollowcube.mapmaker.model.SaveState;
import net.hollowcube.mapmaker.storage.client.MongoClientFactory;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

/**
 * Each player has a save state for every map
 */
public interface SaveStateStorage {

    class NotFoundError extends RuntimeException { }

    static @NotNull SaveStateStorage memory() {
        return new SaveStateStorageMemory();
    }

    static @Blocking @NotNull SaveStateStorage mongo(@NotNull MongoConfig config) {
        var client = MongoClientFactory.get().newClient(config);
        return new SaveStateStorageMongo(client, config);
    }

    @Blocking @NotNull SaveState createSaveState(@NotNull SaveState saveState);

    @Blocking void updateSaveState(@NotNull SaveState saveState);

    @Blocking @NotNull SaveState getLatestSaveState(@NotNull String playerId, @NotNull String mapId, @NotNull SaveState.Type type);

}
