package net.hollowcube.mapmaker.storage;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import net.hollowcube.common.config.MongoConfig;
import net.hollowcube.common.result.Error;
import net.hollowcube.common.result.FutureResult;
import net.hollowcube.mapmaker.model.PlayerData;
import net.hollowcube.mapmaker.storage.client.MongoClientFactory;
import org.jetbrains.annotations.NotNull;

public interface PlayerStorage {

    Error ERR_NOT_FOUND = Error.of("player not found");
    Error ERR_DUPLICATE_ENTRY = Error.of("player already exists");

    class NotFoundError extends RuntimeException {
        public NotFoundError(@NotNull String playerId) {
            super(String.format("Player %s not found", playerId));
        }
    }

    class DuplicateEntryError extends RuntimeException {
    }

    static @NotNull PlayerStorage memory() {
        return new PlayerStorageMemory();
    }

    static @NotNull ListenableFuture<@NotNull PlayerStorage> mongo(@NotNull MongoConfig config) {
        var clientFactory = MongoClientFactory.get();
        return Futures.transform(
                clientFactory.newClient(config),
                client -> new PlayerStorageMongo(client, config),
                Runnable::run
        );
    }

    @NotNull ListenableFuture<@NotNull PlayerData> createPlayer(@NotNull PlayerData player);

    @NotNull FutureResult<@NotNull PlayerData> getPlayerById(@NotNull String id);

    @NotNull ListenableFuture<@NotNull PlayerData> getPlayerByUuid(@NotNull String uuid);

    @NotNull FutureResult<Void> updatePlayer(@NotNull PlayerData player);

    /**
     * Removes the given map from every player with an association.
     */
    @NotNull FutureResult<Void> unlinkMap(@NotNull String mapId);

}
