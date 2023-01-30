package net.hollowcube.mapmaker.storage;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import net.hollowcube.common.config.MongoConfig;
import net.hollowcube.mapmaker.model.SaveState;
import net.hollowcube.mapmaker.storage.client.MongoClientFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Each player has a save state for every map
 */
public interface SaveStateStorage {

    class NotFoundError extends RuntimeException { }

    static @NotNull SaveStateStorage memory() {
        return new SaveStateStorageMemory();
    }

    static @NotNull ListenableFuture<@NotNull SaveStateStorage> mongo(@NotNull MongoConfig config) {
        var clientFactory = MongoClientFactory.get();
        return Futures.transform(
                clientFactory.newClient(config),
                client -> new SaveStateStorageMongo(client, config),
                Runnable::run
        );
    }

    @NotNull ListenableFuture<@NotNull SaveState> createSaveState(@NotNull SaveState saveState);

    @NotNull ListenableFuture<Void> updateSaveState(@NotNull SaveState saveState);

    @NotNull ListenableFuture<@NotNull SaveState> getLatestSaveState(@NotNull String playerId, @NotNull String mapId);

}
