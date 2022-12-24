package net.hollowcube.mapmaker.storage;

import net.hollowcube.common.config.MongoConfig;
import net.hollowcube.common.result.Error;
import net.hollowcube.common.result.FutureResult;
import net.hollowcube.mapmaker.model.SaveState;
import net.hollowcube.mapmaker.storage.client.MongoClientFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Each player has a save state for every map
 */
public interface SaveStateStorage {

    Error ERR_NOT_FOUND = Error.of("savestate not found");
    Error ERR_DUPLICATE_ENTRY = Error.of("savestate already exists");

    static @NotNull SaveStateStorage memory() {
        return new SaveStateStorageMemory();
    }

    static @NotNull FutureResult<SaveStateStorage> mongo(@NotNull MongoConfig config) {
        var clientFactory = MongoClientFactory.get();
        return clientFactory.newClient(config)
                .map(client -> new SaveStateStorageMongo(client, config));
    }

    @NotNull FutureResult<@NotNull SaveState> createSaveState(@NotNull SaveState saveState);

    @NotNull FutureResult<Void> updateSaveState(@NotNull SaveState saveState);

    @NotNull FutureResult<@NotNull SaveState> getLatestSaveState(@NotNull String playerId, @NotNull String mapId);

}
