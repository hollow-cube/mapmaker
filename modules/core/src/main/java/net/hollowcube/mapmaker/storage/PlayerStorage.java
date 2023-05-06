package net.hollowcube.mapmaker.storage;

import net.hollowcube.common.config.MongoConfig;
import net.hollowcube.mapmaker.model.PlayerData;
import net.hollowcube.mapmaker.storage.client.MongoClientFactory;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

@Blocking
public interface PlayerStorage {

    static @NotNull PlayerStorage memory() {
        return new PlayerStorageMemory();
    }

    @Blocking
    static @NotNull PlayerStorage mongo(@NotNull MongoConfig config) {
        var client = MongoClientFactory.get().newClient(config);
        return new PlayerStorageMongo(client, config);
    }

    @Blocking
    @NotNull PlayerData createPlayer(@NotNull PlayerData player) throws DuplicateEntryError;

    @Blocking
    @NotNull PlayerData getPlayerByUuid(@NotNull String uuid) throws NotFoundError;

    @Blocking
    void updatePlayer(@NotNull PlayerData player) throws NotFoundError;


    class NotFoundError extends RuntimeException {
        public NotFoundError(@NotNull String playerId) {
            super(String.format("Player %s not found", playerId));
        }
    }

    class DuplicateEntryError extends RuntimeException {
    }

}
